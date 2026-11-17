# Guía de Configuración y Checklist de Pruebas: TaxiGO

---

## 1. Configuración de Firebase Console

### Pasos Generales:
1. Accede a [Firebase Console](https://console.firebase.google.com/) y crea un nuevo proyecto llamado `taxigo-driver-app`.
2. Registra una aplicación Android con el nombre de paquete `com.example.driver`.
3. Descarga el archivo `google-services.json` de la consola de Firebase y reemplázalo en la carpeta del proyecto Android:
   `app/google-services.json`

### Configurar Firebase Authentication:
1. En la consola de Firebase, ve a **Authentication** > **Métodos de inicio de sesión**.
2. Habilita el proveedor **Correo electrónico / Contraseña**.

### Configurar Cloud Firestore:
1. En la consola de Firebase, ve a **Firestore Database** > **Crear base de datos**.
2. Selecciona la ubicación de la base de datos (ejemplo: `us-central1` o la región más cercana).
3. Despliega las reglas de seguridad y los índices ejecutando el CLI de Firebase desde la raíz del proyecto:
   ```bash
   firebase deploy --only firestore:rules,firestore:indexes
   ```

### Configurar Cloud Functions:
1. Asegúrate de tener instalado Node.js 18+ y Firebase CLI:
   ```bash
   npm install -g firebase-tools
   ```
2. Inicializa sesión en Firebase CLI:
   ```bash
   firebase login
   ```
3. Navega a la carpeta `functions/` e instala dependencias:
   ```bash
   cd functions
   npm install
   npm run build
   ```
4. Despliega las funciones en Firebase Cloud:
   ```bash
   firebase deploy --only functions
   ```

---

## 2. Checklist de Pruebas y Verificación

| Módulo | Pruebas a Realizar | Resultado Esperado |
| :--- | :--- | :--- |
| **Autenticación** | Registrar usuario con rol `PASAJERO` | Crea documento en `users/{uid}` con `role: "PASAJERO"`. |
| **Autenticación** | Registrar usuario con rol `CONDUCTOR` | Crea documento en `users/{uid}` y perfil en `drivers/{uid}`. |
| **Autenticación** | Iniciar sesión con credenciales correctas | Autentica y redirige automáticamente al Home según el rol. |
| **Pasajero** | Obtener ubicación GPS actual | El mapa centra en las coordenadas reales del dispositivo. |
| **Pasajero** | Buscar dirección por texto | Despliega sugerencias de autocompletado y marca en el mapa. |
| **Pasajero** | Solicitar servicio de taxi | Crea documento en `rides/{rideId}` en Firestore con estado `SEARCHING`. |
| **Cloud Function** | Cálculo automático servidor | La Cloud Function `onRequestCreated` asigna tarifa, distancia y ETA real. |
| **Conductor** | Activar disponibilidad "En Línea" | Actualiza `isOnline: true` en `drivers/{uid}` e inicia `DriverLocationService`. |
| **Conductor** | Aceptar solicitud | Actualiza estado a `DRIVER_ASSIGNED` y asocia `driverId` en Firestore. |
| **Conductor** | Avanzar estados del viaje | Transiciona: `DRIVER_ASSIGNED` -> `DRIVER_EN_ROUTE` -> `ARRIVED` -> `IN_PROGRESS` -> `COMPLETED`. |
| **Pasajero** | Calificar al conductor | Al completar el viaje, despliega diálogo de 1-5 estrellas y actualiza `ratings`. |
| **Cloud Function** | Recálculo de calificación | La Cloud Function `onRatingCreated` recalcula el promedio del conductor atómicamente. |
| **Admin** | Panel de Control | Visualiza todos los taxis activos, aprueba/suspende conductores y asigna viajes. |
