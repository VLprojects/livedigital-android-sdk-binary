package space.livedigital.example.devices

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.PUT
import retrofit2.http.Path
import space.livedigital.example.devices.request.RegisterDeviceRequestBody

interface DevicesApiService {

    @PUT("v1/devices/{deviceId}")
    suspend fun registerDevice(
        @Path("deviceId") deviceId: String,
        @Body body: RegisterDeviceRequestBody
    )

    @DELETE("v1/devices/{deviceId}")
    suspend fun deleteDevice(
        @Path("deviceId") deviceId: String
    )
}
