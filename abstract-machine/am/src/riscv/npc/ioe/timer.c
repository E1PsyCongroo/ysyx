#include <am.h>
#include <npc.h>

static uint64_t setup_time;

void __am_timer_init() {
  setup_time = (uint64_t)inl(RTC_ADDR + 4) << 32;
  setup_time = setup_time + inl(RTC_ADDR);
  setup_time /= 3;
}

void __am_timer_uptime(AM_TIMER_UPTIME_T *uptime) {
  uptime->us = (uint64_t)inl(RTC_ADDR + 4) << 32;
  uptime->us = uptime->us + inl(RTC_ADDR);
  uptime->us -= setup_time;
  uptime->us /= 3;
}

void __am_timer_rtc(AM_TIMER_RTC_T *rtc) {
  rtc->second = 0;
  rtc->minute = 0;
  rtc->hour   = 0;
  rtc->day    = 0;
  rtc->month  = 0;
  rtc->year   = 1900;
}