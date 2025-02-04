package com.reactnative.googlecast;

import android.support.annotation.NonNull;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaMetadata;
import com.google.android.gms.cast.MediaStatus;
import com.google.android.gms.cast.framework.media.RemoteMediaClient;
import com.google.android.gms.common.images.WebImage;
import java.util.List;

public class GoogleCastRemoteMediaClientListener
    implements RemoteMediaClient.Listener, RemoteMediaClient.ProgressListener {
  private GoogleCastModule module;
  private boolean playbackStarted;
  private boolean playbackEnded;
  private int currentItemId;

  public GoogleCastRemoteMediaClientListener(GoogleCastModule module) {
    this.module = module;
  }

  @Override
  public void onStatusUpdated() {
    module.runOnUiQueueThread(new Runnable() {
      @Override
      public void run() {
        MediaStatus mediaStatus = module.getMediaStatus();
        if (mediaStatus == null) {
          return;
        }

        if (currentItemId != mediaStatus.getCurrentItemId()) {
          // reset item status
          currentItemId = mediaStatus.getCurrentItemId();
          playbackStarted = false;
          playbackEnded = false;

          List<WebImage> listImages= mediaStatus.getMediaInfo().getMetadata().getImages();
          WritableArray listOfImageUrl = Arguments.createArray();
          for(WebImage vi : listImages){
              listOfImageUrl.pushString(vi.getUrl().toString());
          }

          WritableMap map = Arguments.createMap();
          map.putString("title", mediaStatus.getMediaInfo().getMetadata().getString(MediaMetadata.KEY_TITLE));
          map.putString("subtitle", mediaStatus.getMediaInfo().getMetadata().getString(MediaMetadata.KEY_SUBTITLE));
          map.putArray("images",listOfImageUrl);

          WritableMap rnmessage = Arguments.createMap();
            rnmessage.putString("contentId", mediaStatus.getMediaInfo().getContentId());
            rnmessage.putMap("metadata", map);

          module.emitMessageToRN(GoogleCastModule.MEDIA_METADATA_CHANGED, rnmessage);
        }

        module.emitMessageToRN(GoogleCastModule.MEDIA_STATUS_UPDATED,
                               prepareMessage(mediaStatus));

        if (!playbackStarted &&
            mediaStatus.getPlayerState() == MediaStatus.PLAYER_STATE_PLAYING) {
          module.emitMessageToRN(GoogleCastModule.MEDIA_PLAYBACK_STARTED,
                                 prepareMessage(mediaStatus));
          playbackStarted = true;
        }

        if (!playbackEnded &&
            mediaStatus.getIdleReason() == MediaStatus.IDLE_REASON_FINISHED) {
          module.emitMessageToRN(GoogleCastModule.MEDIA_PLAYBACK_ENDED,
                                 prepareMessage(mediaStatus));
          playbackEnded = true;
        }
      }
    });
  }

  @NonNull
  private WritableMap prepareMessage(MediaStatus mediaStatus) {
    // needs to be constructed for every message from scratch because reusing a
    // message fails with "Map already consumed"
    WritableMap map = Arguments.createMap();
    map.putInt("playerState", mediaStatus.getPlayerState());
    map.putInt("idleReason", mediaStatus.getIdleReason());
    map.putBoolean("muted", mediaStatus.isMute());
    map.putInt("streamPosition", (int)(mediaStatus.getStreamPosition() / 1000));

    MediaInfo info = mediaStatus.getMediaInfo();
    if (info != null) {
      map.putInt("streamDuration", (int) (info.getStreamDuration() / 1000));
    }

    WritableMap message = Arguments.createMap();
    message.putMap("mediaStatus", map);
    return message;
  }

  @Override
  public void onMetadataUpdated() {}

  @Override
  public void onQueueStatusUpdated() {}

  @Override
  public void onPreloadStatusUpdated() {}

  @Override
  public void onSendingRemoteMediaRequest() {}

  @Override
  public void onAdBreakStatusUpdated() {}

  @Override
  public void onProgressUpdated(final long progressMs, final long durationMs) {
    module.runOnUiQueueThread(new Runnable() {
      @Override
      public void run() {
        MediaStatus mediaStatus = module.getMediaStatus();
        if (mediaStatus == null) {
          return;
        }

        if (mediaStatus.getPlayerState() == MediaStatus.PLAYER_STATE_PLAYING) {
          module.emitMessageToRN(
              GoogleCastModule.MEDIA_PROGRESS_UPDATED,
              prepareProgressMessage(progressMs, durationMs));
        }
      }
    });
  }

  @NonNull
  private WritableMap prepareProgressMessage(long progressMs, long durationMs) {
    // needs to be constructed for every message from scratch because reusing a
    // message fails with "Map already consumed"
    WritableMap map = Arguments.createMap();
    map.putInt("progress", (int)progressMs / 1000);
    map.putInt("duration", (int)durationMs / 1000);

    WritableMap message = Arguments.createMap();
    message.putMap("mediaProgress", map);
    return message;
  }
}
