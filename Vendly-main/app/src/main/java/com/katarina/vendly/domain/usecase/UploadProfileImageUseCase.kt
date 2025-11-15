package com.katarina.vendly.domain.usecase

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.InputStream
import kotlin.math.max

class UploadProfileImageUseCase {

    private val client = OkHttpClient()

    //uzima uri slike, optimizuje je, salje na cloudinary, vrati url slike
    suspend operator fun invoke(context: Context, photoUri: Uri): Result<String> =
        withContext(Dispatchers.IO) {   //sve sto se radi salje se na dispecher
            try {
                val maxSide = 1600

                //citamo dimenzije slike i ako je ogromna smanji je
                val bmp: Bitmap = if (Build.VERSION.SDK_INT >= 28) {
                    val src = ImageDecoder.createSource(context.contentResolver, photoUri)
                    ImageDecoder.decodeBitmap(src) { decoder, info, _ ->
                        val w = info.size.width
                        val h = info.size.height
                        val scale = max(1f, max(w, h) / maxSide.toFloat())
                        if (scale > 1f) {
                            decoder.setTargetSize((w / scale).toInt(), (h / scale).toInt())
                        }
                    }
                } else {
                    //samo pravi bitmapu i ako slika ne moze da se ucita vrati gresku
                    val stream: InputStream? = context.contentResolver.openInputStream(photoUri)
                    stream.use { BitmapFactory.decodeStream(it) }
                        ?: return@withContext Result.failure(IllegalStateException("Can't decode selected image"))
                }

                //dodatni resize za starije sisteme
                val finalBmp = if (Build.VERSION.SDK_INT < 28) {
                    val side = max(bmp.width, bmp.height)
                    if (side > maxSide) {
                        val scale = side.toFloat() / maxSide
                        Bitmap.createScaledBitmap(
                            bmp,
                            (bmp.width / scale).toInt(),
                            (bmp.height / scale).toInt(),
                            true
                        )
                    } else bmp
                } else bmp

                // kompresija u jpeg
                val baos = ByteArrayOutputStream()
                finalBmp.compress(Bitmap.CompressFormat.JPEG, 85, baos)
                val data = baos.toByteArray()

                //stavljamo na cloudinary
                val cloudName = "dhvclg8nu"
                val uploadPreset = "vendly_unsigned"
                val url = "https://api.cloudinary.com/v1_1/$cloudName/image/upload"

                //pakuje sliku i podatke u jednu kutiju multipart/form-data
                val body = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart(
                        "file", "vending.jpg",
                        data.toRequestBody("image/jpeg".toMediaTypeOrNull())
                    )
                    .addFormDataPart("upload_preset", uploadPreset)
                    .build()

                //salje http post request
                val request = Request.Builder().url(url).post(body).build()
                client.newCall(request).execute().use { resp ->
                    //provera da li je zahtev uspeo ili ne
                    if (!resp.isSuccessful) {
                        val errBody = resp.body?.string()
                        val msg = runCatching {
                            JSONObject(errBody ?: "")
                                .optJSONObject("error")
                                ?.optString("message")
                        }.getOrNull()
                        return@withContext Result.failure(
                            IllegalStateException("Cloudinary upload failed: ${msg ?: resp.code}")
                        )
                    }
                    val json = JSONObject(resp.body?.string() ?: "")
                    val secureUrl = json.getString("secure_url")
                    Result.success(secureUrl)
                }
            } catch (t: Throwable) {
                Result.failure(t)
            }
        }
}