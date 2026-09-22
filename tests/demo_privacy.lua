-- Run from the repository root: luajit tests/demo_privacy.lua

--- Load the production demo with deterministic engine and SDK stand-ins.
-- @param string platform target operating system
-- @param boolean has_cmp whether the CMP extension is available
-- @return table harness state
local function load_demo(platform, has_cmp)
    local harness = { initialized = 0, applied = 0, resets = 0, grants = 0 }
    local state = { loaded = true, flow_ready = false, revision = 0,
        cmp = { gdpr_applies = true, us_regulation_applies = false } }
    local consent = { storage = { IABTCF_TCString = "real-cmp-choice" } }
    harness.state = state
    harness.self = {}
    local env = setmetatable({
        require = function() return function() return "log\n" end end,
        sys = {
            get_sys_info = function() return { system_name = platform } end,
            get_config_string = function() return "test-code" end,
        },
        msg = { post = function() end },
        gui = {
            get_node = function(name) return name end,
            set_enabled = function() end,
            set_text = function() end,
            set_scale = function() end,
            pick_node = function(node, selected) return node == selected end,
        },
        vmath = { vector4 = function() end, vector3 = function() end },
        hash = function(value) return value end,
        json = { encode = function(value)
            if value.cmp then
                assert(value.cmp == state.cmp and value.consent == consent)
            end
            return "encoded-real-choice"
        end },
        yandexads = {
            set_callback = function(callback) harness.callback = callback end,
            initialize = function() harness.initialized = harness.initialized + 1 end,
            apply_privacy = function(payload)
                assert(payload == "encoded-real-choice")
                harness.applied = harness.applied + 1
                return true
            end,
            reset_ads = function(generation)
                harness.resets = harness.resets + 1
                assert(generation == harness.self.ad_generation)
            end,
            set_user_consent = function() harness.grants = harness.grants + 1 end,
        },
        inmobi_cmp = has_cmp and {
            is_supported = function() return true end,
            initialize = function(options)
                assert(options.p_code == "test-code")
                return true
            end,
            get_status = function() return state end,
            get_consent = function() return consent end,
            show_gdpr = function() state.flow_ready = false; return true end,
            set_listener = function(listener) assert(listener == nil) end,
        } or false,
    }, { __index = _G })
    setfenv(assert(loadfile("main/main.gui_script")), env)()
    env.init(harness.self)
    harness.press = function(button)
        env.on_input(harness.self, "touch", { x = button, y = 0, released = true })
    end
    harness.update = function() env.update(harness.self, 0.016) end
    harness.close = function() env.final(harness.self) end
    return harness
end

local missing = load_demo("Android", false)
missing.press("btn_init")
missing.update()
assert(missing.initialized == 0 and missing.applied == 0)

local android = load_demo("Android", true)
android.press("btn_init")
assert(android.initialized == 0 and android.applied == 0)
android.state.flow_ready = true
android.update()
assert(android.initialized == 1 and android.applied == 1)
android.update()
assert(android.applied == 1)
android.press("btn_set_user_consent")
assert(android.grants == 0 and not android.self.privacy_applied)
android.update()
assert(android.applied == 1)
android.state.revision = 1
android.state.flow_ready = true
android.update()
assert(android.applied == 2 and android.initialized == 1)
android.state.flow_ready = false
android.update()
assert(not android.self.privacy_applied)
android.close()

local ios = load_demo("iPhone OS", false)
ios.press("btn_init")
assert(ios.initialized == 1 and ios.applied == 0 and ios.resets == 0)
ios.close()
print("Demo CMP gating and iOS initialization tests passed")
