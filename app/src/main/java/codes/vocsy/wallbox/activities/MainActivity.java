package codes.vocsy.wallbox.activities;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.HashMap;

import codes.vocsy.wallbox.R;
import codes.vocsy.wallbox.adapters.ImageListAdapter;
import codes.vocsy.wallbox.api.APIClient;
import codes.vocsy.wallbox.api.APIInterface;
import codes.vocsy.wallbox.listener.ScrollListener;
import codes.vocsy.wallbox.models.Post;
import codes.vocsy.wallbox.models.PostList;
import codes.vocsy.wallbox.utils.Util;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import spencerstudios.com.jetdblib.JetDB;
import umairayub.madialog.MaDialog;
import umairayub.madialog.MaDialogListener;

public class MainActivity extends AppCompatActivity implements ImageListAdapter.OnItemClickListener {

    public static final String EXTRA_URL = "imageUrl";
    public static final String EXTRA_CREATOR = "creatorName";
    public static final String EXTRA_SIZE = "imgSize";
    public static final String EXTRA_LIKES = "likeCount";
    public static final String EXTRA_VIEWS = "viewsCount";

    Context ctx = MainActivity.this;
    private ScrollListener scrollListener;
    private APIInterface apiInterface;
    private RecyclerView rv;
    private Button btnApplyFilters;
    private SwipeRefreshLayout mSwipeRefresher;
    private ArrayList<Post> hits;
    private ImageListAdapter imageListAdapter;
    private BottomSheetDialog bottomSheetDialog;
    private String currentQuery = "";
    private RelativeLayout root;
    private TextView tvCheckSavedImages;

    // Filter vars
    private Switch safeSearchSwitch;
    private Spinner ImageTypeSpinner;
    private Spinner OrderSpinner;
    private String[] itemsOrder = {"latest", "popular"};
    private String[] itemsType = {"all", "photo", "illustration", "vector"};
    private String[] itemsCategory = {"all", "fashion", "nature", "backgrounds", "science", "education", "people", "feelings", "religion", "health", "places", "animals", "industry", "food", "computer", "sports", "transportation", "travel", "buildings", "business", "music"};
    private boolean is_safe_search_on;
//    private AdView mAdView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();


        MobileAds.initialize(this, new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(InitializationStatus initializationStatus) {
            }
        });
//
//        mAdView = findViewById(R.id.adView);
//        AdRequest adRequest = new AdRequest.Builder().build();
//        mAdView.loadAd(adRequest);

        tvCheckSavedImages.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(ctx, SavedActivity.class);
                startActivity(i);
            }
        });

        hits = new ArrayList<>();
        rv.setHasFixedSize(true);
        GridLayoutManager mLayoutManager = new GridLayoutManager(this, 2);
        rv.setLayoutManager(mLayoutManager);
        imageListAdapter = new ImageListAdapter(this, hits);
        imageListAdapter.setOnItemClickListener(this);
        rv.setAdapter(imageListAdapter);

        initScrollListener(mLayoutManager);


        if (Util.isNetworkAvailable(ctx)) {
            LoadImages(1, currentQuery, is_safe_search_on, JetDB.getString(ctx, "selected_order", ""), JetDB.getString(ctx, "selected_type", ""));

        } else {
            rv.setVisibility(View.GONE);
            tvCheckSavedImages.setVisibility(View.VISIBLE);
            initSnackbar(R.string.no_internet);
        }

        mSwipeRefresher.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                if (Util.isNetworkAvailable(ctx)) {
                    LoadImages(1, currentQuery, is_safe_search_on, JetDB.getString(ctx, "selected_order", ""), JetDB.getString(ctx, "selected_type", ""));
                    tvCheckSavedImages.setVisibility(View.GONE);
                    rv.setVisibility(View.VISIBLE);
                } else {
                    rv.setVisibility(View.GONE);
                    tvCheckSavedImages.setVisibility(View.VISIBLE);
                    initSnackbar(R.string.no_internet);
                }
            }
        });

    }

    public void initViews() {
        rv = findViewById(R.id.rv);
        mSwipeRefresher = findViewById(R.id.mSwipeRefresh);
        root = findViewById(R.id.root);
        tvCheckSavedImages = findViewById(R.id.tv_chk_imgs);

    }

    private void initSnackbar(int messageId) {
        mSwipeRefresher.setRefreshing(false);
        Snackbar snackbar = Snackbar.make(rv, messageId, Snackbar.LENGTH_INDEFINITE).setAction(R.string.retry, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Util.isNetworkAvailable(ctx)) {
                    resetImageList();
                    mSwipeRefresher.setRefreshing(true);
                    LoadImages(1, currentQuery, is_safe_search_on, JetDB.getString(ctx, "selected_order", ""), JetDB.getString(ctx, "selected_type", ""));
                } else initSnackbar(R.string.no_internet);
            }
        });
        snackbar.show();
    }

    public void LoadImages(int page, String query, boolean is_safe_search_on, String result_order, String result_image_type) {

        HashMap<String, String> map = new HashMap<>();
        map.put("key", "13799911-62a795ec2e29137d307467722");
        map.put("orientation", "vertical");
        map.put("per_page", "200");
        map.put("page", String.valueOf(page));
        map.put("order", result_order);
        map.put("safesearch", String.valueOf(is_safe_search_on));
        map.put("image_type", result_image_type);
        map.put("category", "backgrounds");

        if (!query.equals("")) {
            map.put("q", query);
        }else{
            map.put("q", "sunset");
        }

        mSwipeRefresher.setRefreshing(true);
        apiInterface = APIClient.getClient().create(APIInterface.class);
        Call<PostList> call = apiInterface.getImageResults(map);
        call.enqueue(new Callback<PostList>() {
            @Override
            public void onResponse(Call<PostList> call, Response<PostList> response) {
                PostList postList = response.body();
                addImagesToList(postList);

            }

            @Override
            public void onFailure(Call<PostList> call, Throwable t) {
                initSnackbar(R.string.error);
            }
        });

    }

    private void addImagesToList(PostList response) {
        mSwipeRefresher.setRefreshing(false);
        int position = hits.size();
        hits.addAll(response.getPosts());
        imageListAdapter.notifyItemRangeInserted(position, position + 200);
        if (hits.isEmpty()) {
            initSnackbar(R.string.no_results);
        }

    }

    private void initScrollListener(LinearLayoutManager mLayoutManager) {
        scrollListener = new ScrollListener(mLayoutManager) {
            @Override
            public void onLoadMore(int page) {
                mSwipeRefresher.setRefreshing(true);
                LoadImages(page, currentQuery, is_safe_search_on, JetDB.getString(ctx, "selected_order", ""), JetDB.getString(ctx, "selected_type", ""));
            }
        };
        rv.addOnScrollListener(scrollListener);
    }


    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);

        MenuItem searchItem = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) searchItem.getActionView();
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                resetImageList();
                currentQuery = query;
                LoadImages(1, currentQuery, is_safe_search_on, JetDB.getString(ctx, "selected_order", ""), JetDB.getString(ctx, "selected_type", ""));
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newQuery) {
                if (newQuery.equals("")) {
                    resetImageList();
                    currentQuery = newQuery;
                    LoadImages(1, "", is_safe_search_on, JetDB.getString(ctx, "selected_order", ""), JetDB.getString(ctx, "selected_type", ""));
                }
                return true;
            }

        });
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_filter) {
            openFilterDialog();

        }
        if (item.getItemId() == R.id.action_savedWallpapers) {
            Intent intent = new Intent(ctx, SavedActivity.class);
            startActivity(intent);

        }
        if (item.getItemId() == R.id.action_about) {
           aboutDialog();
        }
        if (item.getItemId() == R.id.action_rate) {
            rateApp();
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onItemClick(int position) {
        Intent detailIntent = new Intent(this, DetailActivity.class);
        Post clickedItem = hits.get(position);

        detailIntent.putExtra(EXTRA_URL, clickedItem.getFullHDURL());
        detailIntent.putExtra(EXTRA_CREATOR, clickedItem.getUser());
        detailIntent.putExtra(EXTRA_SIZE, "W " + clickedItem.getImageWidth() + " x H " + clickedItem.getImageHeight());
        detailIntent.putExtra(EXTRA_LIKES, clickedItem.getLikes());
        detailIntent.putExtra(EXTRA_VIEWS, clickedItem.getViews());

        startActivity(detailIntent);
    }

    private void resetImageList() {
        hits.clear();
        scrollListener.resetCurrentPage();
        imageListAdapter.notifyDataSetChanged();
    }

    private void rateApp(){
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + ctx.getPackageName())));
        }catch (Exception e){
            e.printStackTrace();
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + ctx.getPackageName())));
        }

    }
    private void aboutDialog(){
        new MaDialog.Builder(this)
                .setTitle("About")
                .setMessage("Created by Hardik Thumar && Rohan Patel")
                .setPositiveButtonText("Close")
                .setButtonTextColor(Color.RED)
                .setPositiveButtonListener(new MaDialogListener() {
                    @Override
                    public void onClick() {

                    }
                })
                .build();
    }
    private void openFilterDialog() {
        bottomSheetDialog = new BottomSheetDialog(ctx, R.style.BottomSheetDialogTheme);
        bottomSheetDialog.setContentView(R.layout.filter_dialog);
        safeSearchSwitch = bottomSheetDialog.findViewById(R.id.safe_search_switch);
        ImageTypeSpinner = bottomSheetDialog.findViewById(R.id.image_type_spinner);
        OrderSpinner = bottomSheetDialog.findViewById(R.id.image_order_spinner);
        btnApplyFilters = bottomSheetDialog.findViewById(R.id.btn_apply);


        safeSearchSwitch.setChecked(is_safe_search_on);

//      Adapters
        ArrayAdapter orderAdapter = new ArrayAdapter(this, android.R.layout.simple_spinner_item, itemsOrder);
        orderAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        OrderSpinner.setAdapter(orderAdapter);

        ArrayAdapter typeAdapter = new ArrayAdapter(this, android.R.layout.simple_spinner_item, itemsType);
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        ImageTypeSpinner.setAdapter(typeAdapter);


        //Really could'nt think of any better solution//////////////////////
        for (int i = 0; i < itemsOrder.length; i++) {
            if (itemsOrder[i].equals(JetDB.getString(ctx, "selected_order", ""))) {
                OrderSpinner.setSelection(i);
            }
        }

        for (int i = 0; i < itemsType.length; i++) {
            if (itemsType[i].equals(JetDB.getString(ctx, "selected_type", ""))) {
                ImageTypeSpinner.setSelection(i);
            }
        }
        //////////////////////////////


        OrderSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                JetDB.putString(ctx, itemsOrder[i], "selected_order");
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        ImageTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                JetDB.putString(ctx, itemsType[i], "selected_type");
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });



        safeSearchSwitch.setChecked(is_safe_search_on);
        safeSearchSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean state) {
                is_safe_search_on = state;
            }
        });

        btnApplyFilters.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                resetImageList();
                LoadImages(1, currentQuery, is_safe_search_on, JetDB.getString(ctx, "selected_order", ""), JetDB.getString(ctx, "selected_type", ""));
                bottomSheetDialog.dismiss();

            }
        });
        bottomSheetDialog.show();

    }

    @Override
    protected void onDestroy() {
        JetDB.putString(ctx, "", "selected_category");
        JetDB.putString(ctx, "", "selected_order");
        JetDB.putString(ctx, "", "selected_type");

        super.onDestroy();
    }
}