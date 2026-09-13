package com.vibecheck.lifepulse.di

import com.vibecheck.lifepulse.data.repository.ExpenseRepositoryImpl
import com.vibecheck.lifepulse.data.repository.HabitRepositoryImpl
import com.vibecheck.lifepulse.domain.repository.ExpenseRepository
import com.vibecheck.lifepulse.domain.repository.HabitRepository
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
    abstract fun bindHabitRepository(impl: HabitRepositoryImpl): HabitRepository

    @Binds
    @Singleton
    abstract fun bindExpenseRepository(impl: ExpenseRepositoryImpl): ExpenseRepository
}

