package com.example.driver.di

import com.example.driver.data.repository.AuthRepositoryImpl
import com.example.driver.data.repository.DriverRepositoryImpl
import com.example.driver.data.repository.RideRepositoryImpl
import com.example.driver.domain.repository.AuthRepository
import com.example.driver.domain.repository.DriverRepository
import com.example.driver.domain.repository.RideRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        impl: AuthRepositoryImpl
    ): AuthRepository

    @Binds
    @Singleton
    abstract fun bindRideRepository(
        impl: RideRepositoryImpl
    ): RideRepository

    @Binds
    @Singleton
    abstract fun bindDriverRepository(
        impl: DriverRepositoryImpl
    ): DriverRepository
}
