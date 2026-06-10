package com.droidcode.ide.di

import com.droidcode.ide.lsp.LspClient
import com.droidcode.ide.lsp.LspClientImpl
import com.droidcode.ide.terminal.TerminalSession
import com.droidcode.ide.terminal.TerminalSessionImpl
import com.droidcode.ide.git.GitManager
import com.droidcode.ide.git.GitManagerImpl
import com.droidcode.ide.extensions.ExtensionHost
import com.droidcode.ide.extensions.ExtensionHostImpl
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }
        return OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()
    }

    @Provides
    @Singleton
    fun provideGson(): Gson = GsonBuilder()
        .setLenient()
        .serializeNulls()
        .create()
}

@Module
@InstallIn(SingletonComponent::class)
object CoreServicesModule {

    @Provides
    @Singleton
    fun provideLspClient(okHttp: OkHttpClient, gson: Gson): LspClient = LspClientImpl(okHttp, gson)

    @Provides
    @Singleton
    fun provideTerminalSession(): TerminalSession = TerminalSessionImpl()

    @Provides
    @Singleton
    fun provideGitManager(): GitManager = GitManagerImpl()

    @Provides
    @Singleton
    fun provideExtensionHost(): ExtensionHost = ExtensionHostImpl()
}