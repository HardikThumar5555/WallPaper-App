package codes.vocsy.wallbox.api;

import java.util.Map;

import codes.vocsy.wallbox.models.PostList;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.QueryMap;

public interface APIInterface {

    @GET("/api/")
    Call<PostList> getImageResults(@QueryMap Map<String, String> parameter);
}


