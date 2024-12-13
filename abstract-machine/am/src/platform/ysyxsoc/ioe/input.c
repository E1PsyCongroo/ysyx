#include <am.h>
#include <klib.h>
#include <ysyxsoc.h>

#define KEYDOWN_MASK 0x8000
#define EXT_CODE 0xe0
#define BREAK_CODE 0xf0

#define SCANCODE_KEYS(_)  \
  _(NONE, 0x00) _(ESCAPE, 0x76) _(F1, 0x05) _(F2, 0x06) _(F3, 0x04) _(F4, 0x0c) \
  _(F5, 0x03) _(F6, 0x0b) _(F7, 0x83) _(F8, 0x0a) _(F9, 0x01) _(F10, 0x09) _(F11, 0x78) _(F12, 0x07) \
  _(GRAVE, 0x0e) _(1, 0x16) _(2, 0x1e) _(3, 0x26) _(4, 0x25) _(5, 0x2e) _(6, 0x36)  \
  _(7, 0x3d) _(8, 0x3e) _(9, 0x46) _(0, 0x45) _(MINUS, 0x4e) _(EQUALS, 0x55) _(BACKSPACE, 0x66) \
  _(TAB, 0x0d) _(Q, 0x15) _(W, 0x1d) _(E, 0x24) _(R, 0x2d) _(T, 0x2c) _(Y, 0x35) _(U, 0x3c) \
  _(I, 0x43) _(O, 0x44) _(P,0x4d) _(LEFTBRACKET, 0x54) _(RIGHTBRACKET, 0x5b) _(BACKSLASH, 0x5d) \
  _(CAPSLOCK, 0x58) _(A, 0x1c) _(S, 0x1b) _(D, 0x23) _(F, 0x2b) _(G, 0x34) _(H, 0x33) _(J, 0x3b)  \
  _(K, 0x42) _(L, 0x4b) _(SEMICOLON, 0x4c) _(APOSTROPHE, 0x52) _(RETURN, 0x5A) \
  _(LSHIFT, 0x12) _(Z, 0x1a) _(X, 0x22) _(C, 0x21) _(V, 0x2a) _(B, 0x32) _(N, 0x31) _(M, 0x3a)  \
  _(COMMA, 0x41) _(PERIOD, 0x49) _(SLASH, 0x4a) _(RSHIFT, 0x59) \
  _(LCTRL, 0x14) _(APPLICATION, 0x11) _(LALT, 0x11) _(SPACE, 0x29) _(RALT, 0x111) _(RCTRL, 0x114) \
  _(UP, 0x175) _(DOWN, 0x172) _(LEFT, 0x16b) _(RIGHT, 0x174) _(INSERT, 0x170) _(DELETE, 0x171)  \
  _(HOME, 0x16c) _(END, 0x169) _(PAGEUP, 0x17d) _(PAGEDOWN, 0x17a)

#define SCANCODE_KEYS_MAP(k, c) [c] = AM_KEY_##k,

static int keymap[512] = {SCANCODE_KEYS(SCANCODE_KEYS_MAP)};

void __am_input_keybrd(AM_INPUT_KEYBRD_T *kbd) {
  kbd->keydown = true;
  uint32_t key = inl(PS2_KEY);
  if (key == EXT_CODE) {
    while ((key = inl(PS2_KEY)) == 0) {
      continue;
    }
    if (key == BREAK_CODE) {
      kbd->keydown = false;
      while ((key = inl(PS2_KEY)) == 0) {
        continue;
      }
    }
    kbd->keycode = keymap[0x1 << 8 | key];
  } else {
    if (key == BREAK_CODE) {
      kbd->keydown = false;
      while ((key = inl(PS2_KEY)) == 0) {
        continue;
      }
    }
    kbd->keycode = keymap[key];
  }
}