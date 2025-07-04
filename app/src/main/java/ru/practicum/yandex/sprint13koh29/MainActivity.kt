package ru.practicum.yandex.sprint13koh29

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import ru.practicum.yandex.sprint13koh29.databinding.ActivityMainBinding
import java.util.UUID

class MainActivity : AppCompatActivity() {

    companion object {
        const val TAG = "SPRINT_13"
    }

    val serverApi: ServerApi by lazy {
        ServerApi.create()
    }

    // UI
    private lateinit var binding: ActivityMainBinding
    private val catalogItemsAdapter: CatalogItemsAdapter by lazy {
        CatalogItemsAdapter()
    }
    private val cartItemsAdapter: CartItemsAdapter by lazy {
        CartItemsAdapter()
    }

    // Logic
    private var currentScreenMode: ScreenMode? = null
    var catalogItems = emptyList<CatalogItemViewData>()
    var cartItems = emptyList<CartItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(LayoutInflater.from(this))
        setContentView(binding.root)

        changeCurrentScreenMode(ScreenMode.CATALOG)
        binding.toolbar.setTitle(R.string.catalog_title)
        binding.bottomNavigation.selectedItemId = R.id.catalog
        binding.bottomNavigation.setOnItemSelectedListener {
            onBottomNavigationItemSelected(it.itemId)
        }

        setUpCatalog()
        setUpCart()

        serverApi.getCatalog()
            .enqueue(object : Callback<CatalogResponse> {
                override fun onResponse(
                    call: Call<CatalogResponse>,
                    response: Response<CatalogResponse>,
                ) {
                    val body = response.body()
                    if (response.code() == 200 && body != null) {
                        catalogItems = body.items.map {
                            CatalogItemViewData(
                                item = it,
                                count = null
                            )
                        }
                        catalogItemsAdapter.setItems(catalogItems)
                    } else {
                        Log.d(TAG, "onResponse: error call=$call body=$body")
                    }
                }

                override fun onFailure(call: Call<CatalogResponse>, t: Throwable) {
                    Log.e(TAG, "onFailure: $call $t")
                }

            })
    }

    private fun setUpCatalog() {
        binding.catalogItemsList.apply {
            layoutManager = GridLayoutManager(this@MainActivity, 2)
            adapter = catalogItemsAdapter
            itemAnimator = null
        }

        catalogItemsAdapter.setItems(catalogItems)
        with(catalogItemsAdapter) {
            onAddToCartClickListener = OnAddToCartClickListener { item ->
                catalogItems = catalogItems.map {
                    if (it.id == item.id) {
                        cartItems = cartItems.toMutableList().apply {
                            add(
                                CartItem(
                                    id = UUID.randomUUID().toString(),
                                    catalogItem = it.item,
                                    count = 1
                                )
                            )
                        }
                        updateCartItemsAdapter()
                        it.copy(count = 1)
                    } else {
                        it
                    }
                }
                catalogItemsAdapter.setItems(catalogItems)
            }
            onAddCountClickListener = OnAddCountClickListener { item ->
                catalogItems = catalogItems.map {
                    if (it.id == item.id) {
                        it.copy(count = (it.count ?: 0) + 1)
                    } else {
                        it
                    }
                }
                catalogItemsAdapter.setItems(catalogItems)
            }
            onRemoveCountClickListener = OnRemoveCountClickListener { item ->
                catalogItems = catalogItems.map {
                    if (it.id == item.id) {
                        it.copy(count = (it.count ?: 0) - 1)
                    } else {
                        it
                    }
                }
                catalogItemsAdapter.setItems(catalogItems)
            }
        }
    }

    private fun setUpCart() {
        binding.cartItemsList.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = cartItemsAdapter
            itemAnimator = null
        }

        updateCartItemsAdapter()
        with(cartItemsAdapter) {
            onAddCountClickListener = OnCartAddCountClickListener { item ->
                cartItems = cartItems.map {
                    if (it.id == item.id) {
                        it.copy(count = it.count + 1)
                    } else {
                        it
                    }
                }
                updateCartItemsAdapter()
            }
            onRemoveCountClickListener = OnCartRemoveCountClickListener { item ->
                cartItems = cartItems.map {
                    if (it.id == item.id) {
                        it.copy(count = it.count - 1)
                    } else {
                        it
                    }
                }
                updateCartItemsAdapter()
            }
        }
    }

    private fun onBottomNavigationItemSelected(itemId: Int): Boolean {
        return when (itemId) {
            R.id.catalog -> {
                changeCurrentScreenMode(ScreenMode.CATALOG)
                true
            }

            R.id.cart -> {
                changeCurrentScreenMode(ScreenMode.CART)
                true
            }

            else -> false
        }
    }

    private fun changeCurrentScreenMode(newScreenMode: ScreenMode) {
        if (newScreenMode != currentScreenMode) {
            binding.toolbar.setTitle(newScreenMode.titleResId)
            when (newScreenMode) {
                ScreenMode.CATALOG -> {
                    binding.catalogContainer.visibility = View.VISIBLE
                    binding.cartContainer.visibility = View.GONE
                }

                ScreenMode.CART -> {
                    binding.catalogContainer.visibility = View.GONE
                    binding.cartContainer.visibility = View.VISIBLE
                }
            }
            currentScreenMode = newScreenMode
        }
    }

    private fun updateCartItemsAdapter() {
        cartItemsAdapter.setItems(cartItems)
        if (cartItems.isEmpty()) {
            binding.cartEmptyTitle.visibility = View.VISIBLE
        } else
            binding.cartEmptyTitle.visibility = View.GONE
    }
}