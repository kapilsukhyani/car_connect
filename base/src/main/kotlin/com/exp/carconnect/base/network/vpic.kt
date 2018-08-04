package com.exp.carconnect.base.network

import io.reactivex.Single
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import java.security.cert.X509Certificate
import java.util.*
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

//https://vpic.nhtsa.dot.gov/api/Home
internal class VPICVehicleInfoLoader : VehicleInfoLoader {
    companion object {
        const val BASE_URL = "https://vpic.nhtsa.dot.gov/api/"

    }

    private val retrofit: Retrofit
    private val vpicRestInterface: VPICRestInterface

    init {
        retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(getUnsafeOkHttpClient())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        vpicRestInterface = retrofit.create(VPICRestInterface::class.java)
    }

    override fun loadVehicleInfo(vin: String): Single<VehicleInfo> {
        return vpicRestInterface
                .lookupVIN(vin)
                .retry(2)
                .map {
                    val loadedInfo = it.Results[0]
                    if (loadedInfo.Make.isEmpty() && loadedInfo.Model.isEmpty() && loadedInfo.Manufacturer.isEmpty() && loadedInfo.ModelYear.isEmpty()) {
                        throw RuntimeException("Properties Not Available")
                    }
                    VehicleInfo(loadedInfo.Make, loadedInfo.Model, loadedInfo.Manufacturer, loadedInfo.ModelYear)
                }
    }


    private fun getUnsafeOkHttpClient(): OkHttpClient {
        try {
            // Create a trust manager that does not validate certificate chains
            val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {
                }

                override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {
                }

                override fun getAcceptedIssuers(): Array<X509Certificate> {
                    return emptyArray()
                }

            })

            // Install the all-trusting trust manager
            val sslContext = SSLContext.getInstance("SSL")
            sslContext.init(null, trustAllCerts, java.security.SecureRandom())
            // Create an ssl socket factory with our all-trusting manager
            val sslSocketFactory = sslContext.getSocketFactory()

            val builder = OkHttpClient.Builder()
            builder.sslSocketFactory(sslSocketFactory, trustAllCerts[0] as X509TrustManager)
            builder.hostnameVerifier { _, _ -> true }
            return builder.build()
        } catch (e: Exception) {
            throw RuntimeException(e)
        }

    }

}


internal interface VPICRestInterface {
    @GET("vehicles/decodevinvalues/{vin}?format=json")
    fun lookupVIN(@Path("vin") vin: String): Single<VinLookupResponse>
}

internal data class VinLookupResponse(val Results: Array<Properties>) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as VinLookupResponse

        if (!Arrays.equals(Results, other.Results)) return false

        return true
    }

    override fun hashCode(): Int {
        return Arrays.hashCode(Results)
    }
}

internal data class Properties(val ModelYear: String, val Make: String, val Manufacturer: String, val Model: String)