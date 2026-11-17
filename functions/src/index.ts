import * as functions from "firebase-functions";
import * as admin from "firebase-admin";
import { onCall } from "firebase-functions/v2/https";
import { VertexAI } from "@google-cloud/vertexai";

admin.initializeApp();
const db = admin.firestore();

// Inicializar Vertex AI (Gemini) en Cloud Functions
const vertexAI = new VertexAI({ project: process.env.GCLOUD_PROJECT || "taxigo-driver-app", location: "us-central1" });

// Tarifa Base y Tarifas por Km definidas en el Servidor
const BASE_FARES: { [key: string]: number } = {
  STANDARD: 3500,
  COMFORT: 5500,
  VIP: 8500,
};

const RATES_PER_KM: { [key: string]: number } = {
  STANDARD: 1200,
  COMFORT: 1600,
  VIP: 2200,
};

// 1. Asistente IA Gemini: Sugerencia de ruta y análisis de tráfico inteligente
export const getGeminiRideAdvice = onCall(async (request) => {
  try {
    const model = vertexAI.getGenerativeModel({ model: "gemini-1.5-flash" });
    const pickup = request.data.pickupAddress || "Origen";
    const destination = request.data.destinationAddress || "Destino";
    const category = request.data.category || "STANDARD";

    const prompt = `Actúa como un despachador experto de una app de taxis. Analiza un viaje categoría ${category} desde "${pickup}" hasta "${destination}". Proporciona una recomendación de ruta en español de máximo 2 oraciones optimizada para evitar congestión vehicular.`;

    const result = await model.generateContent(prompt);
    const response = await result.response;
    const adviceText = response.candidates?.[0]?.content?.parts?.[0]?.text || "Ruta óptima principal recomendada.";

    return { success: true, advice: adviceText };
  } catch (error: any) {
    return { success: false, advice: "Siga la ruta principal recomendada por GPS." };
  }
});

// 2. Trigger al crear una solicitud de servicio (rides/{rideId})
export const onRequestCreated = functions.firestore
  .document("rides/{rideId}")
  .onCreate(async (snapshot, context) => {
    const rideId = context.params.rideId;
    const ride = snapshot.data();

    if (!ride) return;

    const pickup = ride.pickupLocation;
    const dest = ride.destinationLocation;
    const category = ride.category || "STANDARD";

    // A. Cálculo Servidor de Distancia (Haversine en km)
    const distanceKm = calculateHaversineKm(
      pickup.latitude,
      pickup.longitude,
      dest.latitude,
      dest.longitude
    );

    // B. Cálculo Servidor de Tarifa y ETA (segundos)
    const baseFare = BASE_FARES[category] || 3500;
    const ratePerKm = RATES_PER_KM[category] || 1200;
    const estimatedFare = Math.round((baseFare + distanceKm * ratePerKm) / 100) * 100;
    const estimatedEtaSeconds = Math.max(180, Math.round((distanceKm / 25) * 3600));

    // C. Actualizar documento de viaje con los valores del Servidor
    await db.collection("rides").doc(rideId).update({
      estimatedDistanceKm: distanceKm,
      estimatedFare: estimatedFare,
      estimatedEtaSeconds: estimatedEtaSeconds,
      updatedAtMillis: Date.now(),
    });

    // D. Notificar Taxis Disponibles Cercanos por FCM
    const onlineDrivers = await db
      .collection("drivers")
      .where("isOnline", "==", true)
      .where("isApproved", "==", true)
      .where("isSuspended", "==", false)
      .get();

    const tokens: string[] = [];
    onlineDrivers.forEach((doc) => {
      const driver = doc.data();
      if (driver.fcmToken) {
        tokens.push(driver.fcmToken);
      }
    });

    if (tokens.length > 0) {
      await admin.messaging().sendEachForMulticast({
        tokens: tokens,
        notification: {
          title: "🚖 Nueva Solicitud de Taxi",
          body: `Origen: ${pickup.addressName || "Cerca a tu posición"}`,
        },
        data: {
          rideId: rideId,
          type: "RIDE_REQUEST",
        },
      });
    }
  });

// 3. Trigger al actualizar el estado de un viaje
export const onRideStatusUpdated = functions.firestore
  .document("rides/{rideId}")
  .onUpdate(async (change, context) => {
    const beforeData = change.before.data();
    const afterData = change.after.data();

    if (beforeData.status === afterData.status) return;

    const passengerId = afterData.passengerId;
    const passengerDoc = await db.collection("users").doc(passengerId).get();
    const fcmToken = passengerDoc.data()?.fcmToken;

    let notificationTitle = "";
    let notificationBody = "";

    switch (afterData.status) {
      case "DRIVER_ASSIGNED":
        notificationTitle = "✅ Taxi Asignado";
        notificationBody = `${afterData.driverName || "Tu conductor"} ha aceptado el viaje.`;
        break;
      case "DRIVER_EN_ROUTE":
        notificationTitle = "🚘 Conductor en camino";
        notificationBody = "Tu taxi se está desplazando hacia tu ubicación.";
        break;
      case "ARRIVED":
        notificationTitle = "📍 ¡Tu taxi ha llegado!";
        notificationBody = "El conductor está afuera esperándote.";
        break;
      case "IN_PROGRESS":
        notificationTitle = "🏁 Viaje iniciado";
        notificationBody = "¡Buen viaje hacia tu destino!";
        break;
      case "COMPLETED":
        notificationTitle = "🎉 Viaje finalizado";
        notificationBody = "Has llegado a tu destino. ¡No olvides calificar la atención!";
        break;
      case "CANCELLED":
        notificationTitle = "❌ Servicio cancelado";
        notificationBody = "La solicitud de servicio fue cancelada.";
        break;
    }

    if (fcmToken && notificationTitle) {
      await admin.messaging().send({
        token: fcmToken,
        notification: {
          title: notificationTitle,
          body: notificationBody,
        },
        data: {
          rideId: context.params.rideId,
          status: afterData.status,
        },
      });
    }
  });

// 4. Trigger al registrar una calificación (ratings/{ratingId})
export const onRatingCreated = functions.firestore
  .document("ratings/{ratingId}")
  .onCreate(async (snapshot) => {
    const rating = snapshot.data();
    if (!rating || !rating.driverId) return;

    const driverRef = db.collection("drivers").doc(rating.driverId);

    await db.runTransaction(async (transaction) => {
      const driverDoc = await transaction.get(driverRef);
      if (!driverDoc.exists) return;

      const currentAverage = driverDoc.data()?.ratingAverage || 5.0;
      const currentCount = driverDoc.data()?.ratingCount || 0;

      const newCount = currentCount + 1;
      const newAverage =
        Math.round(((currentAverage * currentCount + rating.score) / newCount) * 10) / 10;

      transaction.update(driverRef, {
        ratingAverage: newAverage,
        ratingCount: newCount,
      });
    });
  });

// Función matemática auxiliar para cálculo de distancia Haversine
function calculateHaversineKm(
  lat1: number,
  lon1: number,
  lat2: number,
  lon2: number
): number {
  const R = 6371; // Radio de la Tierra en km
  const dLat = ((lat2 - lat1) * Math.PI) / 180;
  const dLon = ((lon2 - lon1) * Math.PI) / 180;
  const a =
    Math.sin(dLat / 2) * Math.sin(dLat / 2) +
    Math.cos((lat1 * Math.PI) / 180) *
      Math.cos((lat2 * Math.PI) / 180) *
      Math.sin(dLon / 2) *
      Math.sin(dLon / 2);
  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
  const d = R * c;
  return Math.max(1.0, Math.round(d * 10) / 10);
}
