package com.fitcoach.app.data.remote

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Url

/**
 * API di ricerca alimentare. Tutte le fonti sono gratuite e non richiedono
 * credenziali (USDA usa la chiave pubblica DEMO_KEY).
 *
 * Gli URL sono passati per intero con @Url perche le due fonti hanno host
 * diversi e non serve un base URL dedicato.
 */
interface FoodApi {

    /**
     * Open Food Facts - ricerca testuale (v1). Endpoint pubblico, nessuna chiave.
     * Documentazione: https://wiki.openfoodfacts.org/API
     */
    @GET
    suspend fun offSearch(
        @Url url: String,
        @Query("search_terms") terms: String,
        @Query("search_simple") simple: Int = 1,
        @Query("action") action: String = "process",
        @Query("json") json: Int = 1,
        @Query("page_size") pageSize: Int = 10,
        @Query("fields") fields: String =
            "code,product_name,product_name_it,brands,nutriments,serving_quantity"
    ): Response<OffSearchResponse>

    /** Open Food Facts - prodotto per codice a barre (v2). */
    @GET
    suspend fun offByBarcode(
        @Url url: String,
        @Query("fields") fields: String = "code,product_name,product_name_it,brands,nutriments"
    ): Response<OffProductResponse>

    /**
     * USDA FoodData Central - ricerca. La chiave DEMO e pubblica e condivisa
     * (limite ~30 richieste/ora): sufficiente per un uso personale.
     * Documentazione: https://fdc.nal.usda.gov/api-guide.html
     */
    @GET
    suspend fun usdaSearch(
        @Url url: String,
        @Query("query") query: String,
        @Query("api_key") apiKey: String = "DEMO_KEY",
        @Query("pageSize") pageSize: Int = 5,
        @Query("dataType") dataType: String = "Foundation,SR Legacy"
    ): Response<UsdaSearchResponse>

    companion object {
        const val OFF_BASE = "https://world.openfoodfacts.org"
        const val OFF_SEARCH_URL = "$OFF_BASE/cgi/search.pl"
        const val OFF_BARCODE_URL = "$OFF_BASE/api/v2/product/"
        const val USDA_SEARCH_URL = "https://api.nal.usda.gov/fdc/v1/foods/search"
    }
}
