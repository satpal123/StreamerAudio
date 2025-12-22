package com.player.streameraudio.presentation.di

import android.content.Context
import com.player.streameraudio.data.api.BackendApiService
import com.player.streameraudio.data.api.TwitchApiService
import com.player.streameraudio.data.api.TwitchOauthApiService
import com.player.streameraudio.data.repository.BackendRepositoryImpl
import com.player.streameraudio.data.repository.TwitchRepositoryImpl
import com.player.streameraudio.data.repository.TwitchValidateRepositoryImpl
import com.player.streameraudio.data.util.Constants
import com.player.streameraudio.domain.repository.BackendRepository
import com.player.streameraudio.domain.repository.TwitchRepository
import com.player.streameraudio.domain.repository.TwitchValidateRepository
import com.player.streameraudio.domain.usecase.GetBackendCategoriesUseCase
import com.player.streameraudio.domain.usecase.GetBackendChannelsUseCase
import com.player.streameraudio.domain.usecase.GetBackendProfileImageUseCase
import com.player.streameraudio.domain.usecase.GetBackendTopStreamsUseCase
import com.player.streameraudio.domain.usecase.GetStreamsByGameUseCase
import com.player.streameraudio.domain.usecase.GetTopGamesUseCase
import com.player.streameraudio.domain.usecase.GetTopStreamsUseCase
import com.player.streameraudio.domain.usecase.GetTwitchStreamsDetailsUseCase
import com.player.streameraudio.domain.usecase.GetTwitchStreamsUseCase
import com.player.streameraudio.domain.usecase.GetTwitchUserUseCase
import com.player.streameraudio.domain.usecase.SearchTwitchChannelsUseCase
import com.player.streameraudio.service.CustomMediaLibrarySessionCallback
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideTwitchApiService():TwitchApiService{
        return Retrofit.Builder()
            .baseUrl(Constants.TWITCH_BASE_URL)
            .addConverterFactory((GsonConverterFactory.create()))
            .build()
            .create(TwitchApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideTwitchValidateApiService(): TwitchOauthApiService {
        return Retrofit.Builder()
            .baseUrl(Constants.TWITCH_OAUTH_VALIDATE_ENDPOINT)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(TwitchOauthApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideBackendApiService(): BackendApiService {
        return Retrofit.Builder()
            .baseUrl(Constants.BACKEND_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(BackendApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideTwitchRepository(apiService: TwitchApiService):TwitchRepository{
        return TwitchRepositoryImpl(apiService)
    }

    @Provides
    @Singleton
    fun provideTwitchValidateRepository(validateApiService: TwitchOauthApiService): TwitchValidateRepository {
        return TwitchValidateRepositoryImpl(validateApiService)
    }

    @Provides
    @Singleton
    fun provideBackendRepository(backendApiService: BackendApiService): BackendRepository {
        return BackendRepositoryImpl(backendApiService)
    }

    @Provides
    @Singleton
    fun provideMediaSessionCallback(
        @ApplicationContext context: Context,
        twitchUserUseCase: GetTwitchUserUseCase,
        twitchStreamsUseCase: GetTwitchStreamsUseCase,
        twitchStreamsDetailsUseCase: GetTwitchStreamsDetailsUseCase,
        getTopStreamsUseCase: GetTopStreamsUseCase,
        getTopGamesUseCase: GetTopGamesUseCase,
        getStreamsByGameUseCase: GetStreamsByGameUseCase,
        searchTwitchChannelsUseCase: SearchTwitchChannelsUseCase,
        getBackendCategoriesUseCase: GetBackendCategoriesUseCase,
        getBackendChannelsUseCase: GetBackendChannelsUseCase,
        getBackendTopStreamsUseCase: GetBackendTopStreamsUseCase,
        getBackendProfileImageUseCase: GetBackendProfileImageUseCase
    ): CustomMediaLibrarySessionCallback = CustomMediaLibrarySessionCallback(
        context,
        twitchUserUseCase,
        twitchStreamsUseCase,
        twitchStreamsDetailsUseCase,
        getTopStreamsUseCase,
        getTopGamesUseCase,
        getStreamsByGameUseCase,
        searchTwitchChannelsUseCase,
        getBackendCategoriesUseCase,
        getBackendChannelsUseCase,
        getBackendTopStreamsUseCase,
        getBackendProfileImageUseCase
    )
}