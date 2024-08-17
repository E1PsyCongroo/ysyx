/***************************************************************************************
* Copyright (c) 2014-2022 Zihao Yu, Nanjing University
*
* NEMU is licensed under Mulan PSL v2.
* You can use this software according to the terms and conditions of the Mulan PSL v2.
* You may obtain a copy of Mulan PSL v2 at:
*          http://license.coscl.org.cn/MulanPSL2
*
* THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND,
* EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT,
* MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
*
* See the Mulan PSL v2 for more details.
***************************************************************************************/

#include <common.h>
#include <device/map.h>
#include <SDL2/SDL.h>

enum {
  reg_freq,
  reg_channels,
  reg_samples,
  reg_sbuf_size,
  reg_init,
  reg_count,
  nr_reg
};

static uint8_t *sbuf = NULL;
static uint32_t *audio_base = NULL;

static void SDL_audio_callback(void* userdata, uint8_t* stream, int len) {
  // static uint32_t position = 0;
  // SDL_LockAudio();
  // uint32_t size = len < (audio_base[reg_count] - position) ? len : (audio_base[reg_count] - position);
  // SDL_memcpy(stream, userdata + position, size);
  // position += size;
  // if (audio_base[reg_count] == position) {
  //   audio_base[reg_count] = 0;
  //   position = 0;
  // }
  // SDL_UnlockAudio();
  SDL_LockAudio();
  uint32_t size = len < audio_base[reg_count] ? len : audio_base[reg_count];
  SDL_memcpy(stream, userdata, size);
  SDL_memmove(userdata, userdata+size, audio_base[reg_count] - size);
  audio_base[reg_count] -= size;
  SDL_UnlockAudio();
}

static void audio_io_handler(uint32_t offset, int len, bool is_write) {
  if (is_write) {
    assert(offset == reg_freq * sizeof(uint32_t) || offset == reg_channels * sizeof(uint32_t) ||
           offset == reg_samples * sizeof(uint32_t) || offset == reg_init * sizeof(uint32_t) ||
           offset == reg_count * sizeof(uint32_t));
    if (offset == reg_init * sizeof(uint32_t)) {
      SDL_InitSubSystem(SDL_INIT_AUDIO);
      SDL_AudioSpec desired;
      SDL_memset(&desired, 0, sizeof(desired));
      desired.freq = audio_base[reg_freq];
      desired.format = AUDIO_S16SYS;
      desired.channels = audio_base[reg_channels];
      desired.samples = audio_base[reg_samples];
      desired.callback = SDL_audio_callback;
      desired.userdata = sbuf;
      SDL_OpenAudio(&desired, NULL);
      SDL_PauseAudio(0);
    }
  }
  else {
    assert(offset == reg_sbuf_size * sizeof(uint32_t) || offset == reg_count * sizeof(uint32_t));
  }
}

void init_audio() {
  uint32_t space_size = sizeof(uint32_t) * nr_reg;
  audio_base = (uint32_t *)new_space(space_size);
#ifdef CONFIG_HAS_PORT_IO
  add_pio_map ("audio", CONFIG_AUDIO_CTL_PORT, audio_base, space_size, audio_io_handler);
#else
  add_mmio_map("audio", CONFIG_AUDIO_CTL_MMIO, audio_base, space_size, audio_io_handler);
#endif

  sbuf = (uint8_t *)new_space(CONFIG_SB_SIZE);
  add_mmio_map("audio-sbuf", CONFIG_SB_ADDR, sbuf, CONFIG_SB_SIZE, NULL);

  audio_base[reg_sbuf_size] = CONFIG_SB_SIZE;
  audio_base[reg_count] = 0;
}
