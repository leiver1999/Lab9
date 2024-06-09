package cr.ac.una.googlelocationservice.viewModel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.gson.Gson
import cr.ac.una.googlelocationservice.entity.Page
import cr.ac.una.googlelocationservice.entity.WikipediaResponse
import cr.ac.una.googlelocationservice.service.WikipediaService
import fuel.Fuel
import fuel.get

class PageViewModel : ViewModel() {
    private var _pages: MutableLiveData<List<Page>?> = MutableLiveData()
    var pages = _pages

    var wikipediaService = WikipediaService()

    private val gson = Gson()


    suspend fun startLoadingPages(title: String) {
        _pages.postValue(listOf())
        var lista = searchPages(title)
        _pages.postValue(lista)
    }

    private suspend fun searchPages(title: String): ArrayList<Page>? {
        return wikipediaService.apiService.getRelatedPages(title).pages
    }

    suspend fun getPagesForNotification(title: String): ArrayList<Page>? {
//        return wikipediaService.apiService.getRelatedPages(title).pages
        val url = "https://en.wikipedia.org/api/rest_v1/page/related/$title"

        val res = Fuel.get(url).body

        val wikipediaResponse = gson.fromJson(res, WikipediaResponse::class.java)

        return wikipediaResponse.pages
    }

    fun getPages() {

    }
}