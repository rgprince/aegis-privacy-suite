package com.aegis.privacy.di

import android.content.Context
import com.aegis.privacy.core.database.AegisDatabase
import com.aegis.privacy.core.engine.BlocklistBridge
import com.aegis.privacy.core.engine.VpnBlocklistAdapter
import com.aegis.privacy.core.parser.HostsFileParser
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * Hilt module providing application-wide dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AegisDatabase {
        return AegisDatabase.getDatabase(context)
    }
    
    @Provides
    @Singleton
    fun provideHostsFileParser(): HostsFileParser {
        return HostsFileParser()
    }
    
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }
    
    @Provides
    @Singleton
    fun provideBlocklistBridge(
        vpnAdapter: VpnBlocklistAdapter
    ): BlocklistBridge {
        return vpnAdapter
    }
    
    @Provides
    @Singleton
    fun provideRootUtils(): com.aegis.privacy.util.RootUtils {
        return com.aegis.privacy.util.RootUtils()
    }
}
