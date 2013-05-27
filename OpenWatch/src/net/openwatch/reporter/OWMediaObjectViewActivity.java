package net.openwatch.reporter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnPreparedListener;
import android.media.MediaPlayer.OnVideoSizeChangedListener;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.*;
import android.view.ViewGroup.LayoutParams;
import android.widget.*;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.example.touch.TouchImageView;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.assist.ImageLoadingListener;
import com.nostra13.universalimageloader.core.assist.ImageSize;
import com.nostra13.universalimageloader.core.assist.SimpleImageLoadingListener;
import net.openwatch.reporter.FeedFragmentActivity.TabsAdapter;
import net.openwatch.reporter.constants.Constants;
import net.openwatch.reporter.constants.Constants.CONTENT_TYPE;
import net.openwatch.reporter.constants.Constants.HIT_TYPE;
import net.openwatch.reporter.constants.Constants.MEDIA_TYPE;
import net.openwatch.reporter.constants.DBConstants;
import net.openwatch.reporter.http.OWServiceRequests;
import net.openwatch.reporter.model.OWServerObject;
import net.openwatch.reporter.model.OWServerObjectInterface;
import net.openwatch.reporter.share.Share;
import org.json.JSONObject;

import java.util.ArrayList;

public class OWMediaObjectViewActivity extends SherlockFragmentActivity {

	private static final String TAG = "RecordingViewActivity";
	
	private ArrayList<Fragment> attached_fragments = new ArrayList<Fragment>();

	TabHost mTabHost;
	ViewPager mViewPager;
	TabsAdapter mTabsAdapter;
	View media_view;

	public static int model_id = -1;
	int server_id = -1;
	boolean is_local = false;
	boolean is_user_owner = false;
	boolean video_playing = false;
	boolean is_landscape = false;
	boolean media_view_inflated = false;
	
	CONTENT_TYPE content_type;
	MEDIA_TYPE media_type;
	
	LayoutInflater inflater;

	@SuppressLint("NewApi")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		/*
		 * Lock Activity portrait for now
		if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
			requestWindowFeature(Window.FEATURE_NO_TITLE);
			is_landscape = true;
		}else
			getSupportActionBar().setDisplayHomeAsUpEnabled(true);
			*/
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		setContentView(R.layout.activity_local_recording_view);
		//media_view = findViewById(R.id.media_object_media_view_stub);

		inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		
		try {
			model_id = getIntent().getExtras().getInt(Constants.INTERNAL_DB_ID);
			OWServerObject media_obj = OWServerObject.objects(this, OWServerObject.class).get(model_id);
			content_type = media_obj.getContentType(getApplicationContext());
			media_type = media_obj.getMediaType(getApplicationContext());
			server_id = media_obj.server_id.get();
			setupMediaViewForOWServerObject(media_obj);
			SharedPreferences prefs = this.getSharedPreferences(Constants.PROFILE_PREFS, MODE_PRIVATE);
			int user_id = prefs.getInt(DBConstants.USER_SERVER_ID, 0);
			if(user_id != 0){
				Log.i("UserRecCheck", "user_id " + user_id + "media_user_id: " + media_obj.user.get(getApplicationContext()).server_id.get());
				if (media_obj.user.get(getApplicationContext()) != null && user_id == media_obj.user.get(getApplicationContext()).server_id.get()){
					is_user_owner = true;
				}
			}
			
			if(!is_landscape && media_obj != null && media_obj.title.get() != null)
				this.getSupportActionBar().setTitle(media_obj.title.get());
			
			updateOWMediaObject(media_obj);
			/*
			String video_path = null;
			if(video_path != null){
				Log.i(TAG, "Video uri: " + video_path);
				setupVideoView(R.id.media_object_media_view, video_path);
			} else{
				Log.e(TAG, "Recording has no local or remote video uri specified");
			}
			*/
			if(server_id > 0)
				OWServiceRequests.increaseHitCount(getApplicationContext(), server_id, model_id, media_obj.getContentType(getApplicationContext()), media_obj.getMediaType(getApplicationContext()), HIT_TYPE.VIEW);
			// Log.i(TAG, "got model_id : " + String.valueOf(model_id));
		} catch (Exception e) {
			Log.e(TAG, "Could not load Intent extras");
			e.printStackTrace();
		}

		if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT){
			mTabHost = (TabHost) findViewById(android.R.id.tabhost);
			mTabHost.setup();
			mTabHost.requestFocus();
			mViewPager = (ViewPager) findViewById(R.id.pager);
			mTabsAdapter = new TabsAdapter(this, mTabHost, mViewPager);
			
			Bundle fragBundle = new Bundle(1);
			fragBundle.putBoolean(Constants.IS_LOCAL_RECORDING, is_local);
			fragBundle.putBoolean(Constants.IS_USER_RECORDING, is_user_owner);
			mTabsAdapter.addTab(mTabHost.newTabSpec(getString(R.string.tab_info))
					.setIndicator(inflateCustomTab(getString(R.string.tab_info))),
					OWMediaObjectInfoFragment.class, fragBundle);
					
			mTabsAdapter.addTab(mTabHost.newTabSpec(getString(R.string.tab_map))
					.setIndicator(inflateCustomTab(getString(R.string.tab_map))), MapFragment.class,
					null);
	
			if (savedInstanceState != null) {
				mTabHost.setCurrentTabByTag(savedInstanceState.getString("tab"));
			}
		}
	}


	@Override
	protected void onResume() {
		super.onResume();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getSupportMenuInflater().inflate(R.menu.server_object, menu);
		return true;
	}
	
	@Override
	public boolean onPrepareOptionsMenu(Menu menu){
		if(!is_local){
			//menu.removeItem(R.id.menu_delete);
			
		}
		if(!is_user_owner){
			menu.removeItem(R.id.menu_save);
		}
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			finish();
			return true;
		case R.id.menu_save:
			finish();
			return true;
		case R.id.menu_share:
			if(server_id > 0){
                OWServerObject server_obj = OWServerObject.objects(this, OWServerObject.class).get(model_id);
				Share.showShareDialogWithInfo(this, "Share This Media!", server_obj.getTitle(getApplicationContext()), OWUtils.urlForOWServerObject(server_obj, getApplicationContext()));
				OWServiceRequests.increaseHitCount(getApplicationContext(), server_id, model_id, content_type, media_type, HIT_TYPE.CLICK);
			}
			break;
        /*
		case R.id.menu_delete:
			OWMediaObjectViewFunctions.showDeleteDialog(this, model_id);
			break;
		*/
		}
		return super.onOptionsItemSelected(item);
	}
	
	private void updateOWMediaObject(OWServerObject server_object){
		// remote object, and need to get media_url
		final Context c = this.getApplicationContext();
		OWServiceRequests.getOWServerObjectMeta(c, server_object, "", new JsonHttpResponseHandler(){

            @Override
            public void onSuccess(JSONObject response) {
                Log.i(TAG,"getOWServerObject in ViewActivity success! " + response.toString());
                OWServerObject serverObject = OWServerObject.objects(c, OWServerObject.class).get(model_id);
                OWServerObjectInterface child = (OWServerObjectInterface) serverObject.getChildObject(c);
                if (child != null && response.has("id")){
                    child.updateWithJson(c, response);
                    setupMediaViewForOWServerObject(serverObject);
                    if(getMapFragment() != null)
                        ((OWMediaObjectBackedEntity) OWMediaObjectViewActivity.this.getMapFragment() ).populateViews(serverObject, c);
                    if(getInfoFragment() != null)
                        ((OWMediaObjectBackedEntity) OWMediaObjectViewActivity.this.getInfoFragment() ).populateViews(serverObject, c);
                }
            }

            @Override
            public void onFailure(Throwable e, JSONObject errorResponse) {
                Log.i(TAG,"getOWServerObject in ViewActivity success!" + errorResponse.toString());
                e.printStackTrace();
            }

        });
	}
/*
	public void setVideoViewVisible(boolean visible) {
		View video = findViewById(R.id.media_object_media_view);
		if (visible) {
			video.setVisibility(View.VISIBLE);
		} else {
			video.setVisibility(View.GONE);
		}

	}
	*/
	
	private View inflateCustomTab(String tab_title){
    	ViewGroup tab = (ViewGroup) inflater.inflate(R.layout.tab_indicator_openwatch, (ViewGroup) this.findViewById(android.R.id.tabs), false);
		((TextView)tab.findViewById(R.id.title)).setText(tab_title);
		return tab;
	}

	public void setupVideoView(int view_id, String filepath) {
        if(filepath == null){
            Log.e(TAG, "setupVideoView uri is null");
            return;
        }
		VideoView video_view = (VideoView) findViewById(view_id);
		video_view.setVideoURI(Uri.parse(filepath));
		video_view.setOnPreparedListener(new OnPreparedListener() {
			@Override
			public void onPrepared(MediaPlayer mp) {
				mp.setOnVideoSizeChangedListener(new OnVideoSizeChangedListener() {
					@Override
					public void onVideoSizeChanged(MediaPlayer mp, int width,
							int height) {
						VideoView video_view = (VideoView) findViewById(R.id.media_object_media_view);
						//video_view.setVisibility(View.VISIBLE);
						(findViewById(R.id.progress_container)).setVisibility(View.GONE);
						video_view.setLayoutParams( new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
						MediaController mc = new MediaController(
								OWMediaObjectViewActivity.this);
						video_view.setMediaController(mc);
						mc.setAnchorView(video_view);
						video_view.requestFocus();
						video_view.start();
						video_playing = true;
					}
				});
			}
		});
        video_view.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                video_playing = false;
            }
        });
		video_view.start();
	}
	
	public void setupImageView(int view_id, String uri){
        if(uri == null){
            Log.e(TAG, "setupImageView uri is null");
            return;
        }
		if(is_local && !uri.contains("file:\\/\\/"))
            uri = "file://" + uri;
        final String absolute_uri = uri;
		Log.i("setupImageView", uri);
		//ImageView v = (ImageView) findViewById(view_id);
		//ImageSize size = getMediaViewDimens();
		
		ImageSize size = new ImageSize(640, 480);
		Log.i("setupImageView", String.format("ImageView dimen: %d x %d ", size.getWidth(), size.getHeight()));
		ImageLoader.getInstance().loadImage(absolute_uri, size, null, new SimpleImageLoadingListener() {
		    @Override
		    public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
		    	Log.i("setupImageView", String.format("got bitmap, loading into imageView of size %dx%d",media_view.getWidth(), media_view.getHeight()));
		       ((ImageView) media_view).setImageBitmap(loadedImage);
		       return;
		    }
		});

        final ViewGroup container = (ViewGroup) this.findViewById(R.id.container);

        final Context c = this.getApplicationContext();

        media_view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LayoutInflater layoutInflater = (LayoutInflater) c.getSystemService(LAYOUT_INFLATER_SERVICE);
                View popupView = layoutInflater.inflate(R.layout.image_popup, null);
                final PopupWindow popupWindow = new PopupWindow(popupView,LayoutParams.FILL_PARENT,LayoutParams.FILL_PARENT, true);
                popupWindow.setAnimationStyle(R.style.FadeInAndOutAnimation);
                popupView.findViewById(R.id.image).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        popupWindow.dismiss();
                    }
                });
                popupWindow.setBackgroundDrawable(new BitmapDrawable()); // Now the popUp responds to onKey
                // http://stackoverflow.com/questions/3121232/android-popup-window-dismissal
                popupWindow.setContentView(popupView);



                Display display = getWindowManager().getDefaultDisplay();
                Point size = new Point();
                display.getSize(size);
                ImageSize fullscreen_size = new ImageSize(size.x, size.y);
                ImageLoader.getInstance().displayImage(absolute_uri, (ImageView)popupView.findViewById(R.id.image),new ImageLoadingListener() {
                    @Override
                    public void onLoadingStarted(String imageUri, View view) {

                    }

                    @Override
                    public void onLoadingFailed(String imageUri, View view, FailReason failReason) {

                    }

                    @Override
                    public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                        popupWindow.showAtLocation(container, Gravity.CENTER,0,0);
                    }

                    @Override
                    public void onLoadingCancelled(String imageUri, View view) {

                    }
                });
                //ImageLoader.getInstance().displayImage(absolute_uri, (ImageView)popupView.findViewById(R.id.image));
            }
        });
		
	}
	
	public void onAttachFragment (Fragment fragment){
    	if(OWMediaObjectBackedEntity.class.isInstance(fragment))
    		attached_fragments.add((Fragment)fragment);
    }
	
	public Fragment getMapFragment(){
		if(attached_fragments.size() == 2)
			return attached_fragments.get(1);
		return null;
	}
	
	public Fragment getInfoFragment(){
		if(attached_fragments.size() == 2)
			return attached_fragments.get(0);
		return null;
	}
	
	public void setupMediaViewForOWServerObject(OWServerObject object){
		String media_path = "";
        Log.i(TAG, String.format("setupMediaView. lat:%f, lon:%f", object.getLat(getApplicationContext()), object.getLon(getApplicationContext()) ));
		switch(object.getMediaType(getApplicationContext())){
		case VIDEO:
            if(!video_playing){
                if( object.local_video_recording.get(getApplicationContext()) != null ){
                    // This is a local recording, attempt to play HQ file
                    is_local = true;
                    media_path = object.local_video_recording.get(getApplicationContext()).hq_filepath.get();

                } else if( object.video_recording.get(getApplicationContext()) != null && object.video_recording.get(getApplicationContext()).media_url.get() != null){
                    // remote recording, and video_url present
                    media_path = object.video_recording.get(getApplicationContext()).media_url.get();
                }
                inflateMediaView(R.layout.video_media_view);
                Log.i(TAG, String.format("setupMediaView. media_url: %s", media_path));
                this.setupVideoView(R.id.media_object_media_view, media_path);
            }
			break;
		case AUDIO:
			media_path = object.audio.get(getApplicationContext()).getMediaFilepath(getApplicationContext());
			if(media_path == null || media_path.compareTo("") == 0){
				media_path = object.audio.get(getApplicationContext()).media_url.get();
				is_local = false;
			}else
				is_local = true;
			inflateMediaView(R.layout.video_media_view);
            Log.i(TAG, String.format("setupMediaView. media_url: %s", media_path));
			this.setupVideoView(R.id.media_object_media_view, media_path);
			break;
		case PHOTO:
			media_path = object.photo.get(getApplicationContext()).getMediaFilepath(getApplicationContext());
			if(media_path == null || media_path.compareTo("") == 0){
				media_path = object.photo.get(getApplicationContext()).media_url.get();
				is_local = false;
			} else
				is_local = true;
			inflateMediaView(R.layout.photo_media_view);
            Log.i(TAG, String.format("setupMediaView. media_url: %s", media_path));
			this.setupImageView(R.id.media_object_media_view, media_path);
			break;
		}
		
	}
	
	public void inflateMediaView(int layoutResource){
        if(!media_view_inflated){
            ViewStub stub = (ViewStub) findViewById(R.id.media_object_media_view_stub);
            stub.setLayoutResource(layoutResource);
            media_view = stub.inflate();
            //setMediaViewDimens();
        }
		media_view_inflated = true;
	}
	
	@SuppressLint("NewApi")
	public void setMediaViewDimens(){
		if(!is_landscape){
			ImageSize size = getMediaViewDimens();
			media_view.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, size.getHeight()));
		}
	}
	
	@SuppressLint("NewApi")
	public ImageSize getMediaViewDimens(){
		if(!is_landscape){
			//make a guess of the videoView height so when we fit it to the loaded video
			// the swap isn't too jarring
			Display display = getWindowManager().getDefaultDisplay();
			int height;
			int width;
			if(Build.VERSION.SDK_INT >=11){
				Point size = new Point();
				display.getSize(size);
				height = (int)(size.x * 3 / 4.0); // assuming 4:3 aspect
				width = size.y;
			}else{
				height = (int)(display.getWidth() * 3 / 4.0); 
				width = (int)(display.getWidth());
			}
			return new ImageSize(width, height);
		}
		return null;
	}

}
