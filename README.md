> Personal fork for our own company projects. Not intended for an upstream pull request.

# Yandex Mobile Ads SDK for Defold
_“This plugin is not endorsed or sponsored by Yandex LLC. This is an independent, unofficial plugin. “_

Defold [native extension](https://www.defold.com/manuals/extensions/) which provides access to Yandex Mobile Ads SDK functionality on Android and iOS.

# Setup

## How do I use this extension?

You can use the Yandex Mobile Ads SDK for Defold extension in your own project by adding this project as a [Defold library dependency](http://www.defold.com/manuals/libraries/).
Open your game.project file and in the dependencies field under project add:

>https://github.com/aglitchman/defold-yandex-easy-monetization-fork/archive/refs/heads/main.zip
For reproducible dependencies, use an archive URL for a specific commit of this fork.

Please, read [Android API docs](https://yandex.ru/support2/mobile-ads/en/dev/android/quick-start) and [iOS API docs](https://yandex.ru/support2/mobile-ads/en/dev/ios/quick-start)

This repository also acts as a sample app. See `main/main.gui_script`.

## Fork changes (Android)

Android uses Yandex Mobile Ads **8.4.0** with Unity Ads, LevelPlay, Mintegral, Bigo, Vungle and myTarget mediation. Google and AppLovin adapters are excluded. Set `[android] minimum_sdk_version` to **23** or higher; the resolved advertising support libraries require it. iOS keeps the upstream implementation and SDK configuration; the Android privacy additions below are not exposed on iOS.

Automatic Android SDK initialization is disabled. Before `initialize()`, call `apply_privacy(json_payload)` and check its boolean result. Until privacy is applied successfully, Android initialization and ad loading/showing remain blocked. The existing `set_user_consent()` API does not replace this step.

`json_payload` contains:

- `cmp.gdpr_applies` and `cmp.us_regulation_applies`: known booleans from CMP status.
- `consent.storage.IABTCF_TCString`: a nonempty TC string when GDPR applies. The SDKs read real TCF/Additional Consent preferences written by CMP; the payload is not a replacement for that storage.
- `consent.us_privacy.known` and `consent.us_privacy.opt_out`: decoded applicable US choices when US regulations apply. Unknown choices fail rather than granting consent.

The data shape matches [InMobi CMP for Defold](https://github.com/indiesoftby/defold-inmobi-cmp). After its native flow reports ready, pass `json.encode({ cmp = status.cmp, consent = inmobi_cmp.get_consent() })`. Flow completion alone is not a universal consent grant. Configure the actual vendors and applicable regulations in CMP.

On a privacy change, call `reset_ads(generation)` with a new integer generation before requesting fresh ads. It revokes the applied state, clears cached ads and replaces loaders. Apply the new choices before loading again. Android callback messages include `generation`; ignore messages whose generation differs from your current one. Callbacks from obsolete loaders cannot refill the ad cache.

For the Android demo, add the InMobi dependency below and configure `[inmobi_cmp] p_code` and a matching registered `[android] package` at build time:

```text
https://github.com/indiesoftby/defold-inmobi-cmp/archive/refs/heads/main.zip
```

The demo's Init button attaches CMP and waits for its flow before initializing ads. Without configured CMP it logs setup instructions and does not load ads. On Android the consent button reopens the applicable CMP form instead of granting blanket consent. Applications where both GDPR and US settings apply should expose both form methods. The extension itself does not require a particular CMP implementation.

# Lua API

## Methods

	yandexads.set_callback(listener) -- listener: function
	yandexads.initialize()
	yandexads.apply_privacy(json_payload) -- Android only; returns boolean
	yandexads.reset_ads(generation) -- Android only; generation: integer
	yandexads.enable_logging()
	yandexads.set_user_consent(consent) -- consent: boolean

	yandexads.load_banner(adUnitId, width, height) -- adUnitId: string, width: int, height: int
	yandexads.is_banner_loaded() -- return: boolean
	yandexads.show_banner(position) -- position: int
	yandexads.hide_banner()
	yandexads.destroy_banner()

	yandexads.load_interstitial(adUnitId) -- adUnitId: string
	yandexads.is_interstitial_loaded() -- return: boolean
	yandexads.show_interstitial()

	yandexads.load_rewarded(adUnitId) -- adUnitId: string
	yandexads.is_rewarded_loaded() -- return: boolean
	yandexads.show_rewarded()

## Constants

	yandexads.MSG_ADS_INITED
	yandexads.MSG_INTERSTITIAL
	yandexads.MSG_REWARDED
	yandexads.MSG_BANNER

	yandexads.EVENT_LOADED
	yandexads.EVENT_ERROR_LOAD
	yandexads.EVENT_SHOWN
	yandexads.EVENT_DISMISSED
	yandexads.EVENT_CLICKED
	yandexads.EVENT_IMPRESSION
	yandexads.EVENT_NOT_LOADED
	yandexads.EVENT_REWARDED
	yandexads.EVENT_DESTROYED
	yandexads.EVENT_COMPLETED

	yandexads.POS_NONE
	yandexads.POS_TOP_LEFT
	yandexads.POS_TOP_CENTER
	yandexads.POS_TOP_RIGHT
	yandexads.POS_BOTTOM_LEFT
	yandexads.POS_BOTTOM_CENTER
	yandexads.POS_BOTTOM_RIGHT
	yandexads.POS_CENTER

# How to use ?

1. Set an event handling callback
2. On Android, wait for CMP and successfully apply its current privacy signals as described above.
3. Run initialization, then load the desired ad format after the initialization callback.
```lua
local function listener(self, message_id, message)
	if message_id == yandexads.MSG_ADS_INITED then
		-- Extension is ready to load ads
	end
end

yandexads.set_callback(listener) -- (1)
yandexads.initialize() -- (2)
yandexads.set_user_consent(true) -- Call if user has given consent
```

`yandexads.enable_logging()` provides additional debug logging to the console from the SDK itself.

## BANNER

```lua
local function listener(self, message_id, message)
	if message_id == yandexads.MSG_ADS_INITED then
		yandexads.load_banner('demo-banner-yandex')
	end

	if message_id == yandexads.MSG_BANNER then
		if event == yandexads.EVENT_LOADED then
			yandexads.show_banner(yandexads.BOTTOM_CENTER) -- optional position(default BOTTOM_CENTER)
		end
	end
end
```

The default position is bottom center.

When loading a banner if you provide width only, the actual banner size is calculated as ["adaptive sticky banner"](https://yandex.ru/support2/mobile-ads/en/dev/android/adaptive-sticky-banner):

	Adaptive sticky banners provide maximum efficiency by optimizing the size of the ad on each device. This ad type lets developers set a maximum allowable ad width, though the optimal ad size is still determined automatically. The height of the adaptive sticky banner shouldn't exceed 15% of the screen height.

If you provide both width and height, the actual banner size is calculated as ["adaptive inline banner"](https://yandex.ru/support2/mobile-ads/en/dev/android/adaptive-inline-banner):

	This type of advertising allows developers to specify the maximum allowable width and height of the ad, while the most optimal ad size is determined automatically. To select the best ad size, built-in adaptive banners use the maximum height rather than the fixed height. This leads to potential performance improvement.

On iOS only `yandexads.POS_TOP_CENTER` and `yandexads.POS_BOTTOM_CENTER` are supported.

## INTERSTITIAL

```lua
local function listener(self, message_id, message)
	if message_id == yandexads.MSG_ADS_INITED then
		yandexads.load_interstitial('demo-interstitial-yandex')
	end

	if message_id == yandexads.MSG_INTERSTITIAL then
		if event == yandexads.EVENT_LOADED then
			yandexads.show_interstitial()
		end
	end
end
```


## REWARDED

```lua
local function listener(self, message_id, message)
	if message_id == yandexads.MSG_ADS_INITED then
	   yandexads.load_rewarded('demo-rewarded-yandex')
	end

	if message_id == yandexads.MSG_REWARDED then
		if event == yandexads.EVENT_LOADED then
			yandexads.show_rewarded()
		elseif event == yandexads.EVENT_REWARDED then
			print('Reward type: ' .. message.type)
			print('Reward amount: ' .. message.amount)
		end
	end
end
```

## Build and checks

Run `python tools/build.py android --variant debug` or `--variant release`. Each run resolves the current stable Defold release from `https://d.defold.com/stable/info.json`. Optional local settings can be passed with `--settings local.project`. Build logs and resolved engine details are saved in `.cache/`; bundles are saved in `bundles/`.

Run `luajit tests/demo_privacy.lua` to check the demo consent flow and retained iOS initialization, and `python tools/test_privacy.py` for host-JVM privacy signal tests. They use Android/advertising API stand-ins and do not establish live mediation delivery. iOS sources are preserved, but these Android changes have not been validated with an iOS build or device run.
