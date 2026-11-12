package com.player.streameraudio.presentation.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import com.player.streameraudio.BuildConfig
import com.player.streameraudio.data.auth.KeystoreTokenStore
import com.player.streameraudio.data.auth.TokenStore
import com.player.streameraudio.data.playback.StreamResolver
import com.player.streameraudio.data.network.NetworkMonitor
import com.player.streameraudio.data.api.BackendApiService
import com.player.streameraudio.data.api.TwitchApiService
import com.player.streameraudio.data.api.TwitchOauthApiService
import com.player.streameraudio.data.repository.BackendRepositoryImpl
import com.player.streameraudio.data.repository.PlaybackSettingsRepositoryImpl
import com.player.streameraudio.data.settings.UserSettings
import com.player.streameraudio.data.settings.UserSettingsSerializer
import com.player.streameraudio.data.repository.TwitchRepositoryImpl
import com.player.streameraudio.data.repository.TwitchValidateRepositoryImpl
import com.player.streameraudio.data.util.Constants
import com.player.streameraudio.domain.repository.BackendRepository
import com.player.streameraudio.domain.repository.PlaybackSettingsRepository
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
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC
            else HttpLoggingInterceptor.Level.NONE
            redactHeader("Authorization")
            redactHeader("Client-Id")
        }
        return OkHttpClient.Builder()
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .callTimeout(25, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .build()
    }

    @Provides
    @Singleton
    fun provideTwitchApiService(okHttpClient: OkHttpClient):TwitchApiService{
        return Retrofit.Builder()
            .baseUrl(Constants.TWITCH_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory((GsonConverterFactory.create()))
            .build()
            .create(TwitchApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideTwitchValidateApiService(okHttpClient: OkHttpClient): TwitchOauthApiService {
        return Retrofit.Builder()
            .baseUrl(Constants.TWITCH_OAUTH_VALIDATE_ENDPOINT)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(TwitchOauthApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideBackendApiService(okHttpClient: OkHttpClient): BackendApiService {
        return Retrofit.Builder()
            .baseUrl(Constants.BACKEND_BASE_URL)
            .client(okHttpClient)
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
    fun provideUserSettingsDataStore(@ApplicationContext context: Context): DataStore<UserSettings> =
        DataStoreFactory.create(
            serializer = UserSettingsSerializer(),
            corruptionHandler = ReplaceFileCorruptionHandler { UserSettings() },
            produceFile = { File(context.filesDir, "datastore/user_settings.json") }
        )

    @Provides
    @Singleton
    fun providePlaybackSettingsRepository(impl: PlaybackSettingsRepositoryImpl): PlaybackSettingsRepository = impl

    @Provides
    @Singleton
    fun provideTokenStore(impl: KeystoreTokenStore): TokenStore = impl

    @Provides
    @Singleton
    fun provideMediaSessionCallback(
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
        getBackendProfileImageUseCase: GetBackendProfileImageUseCase,
        tokenStore: TokenStore,
        playbackSettingsRepository: PlaybackSettingsRepository,
        streamResolver: StreamResolver,
        networkMonitor: NetworkMonitor
    ): CustomMediaLibrarySessionCallback = CustomMediaLibrarySessionCallback(
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
        getBackendProfileImageUseCase,
        tokenStore,
        playbackSettingsRepository,
        streamResolver,
        networkMonitor
    )
}
