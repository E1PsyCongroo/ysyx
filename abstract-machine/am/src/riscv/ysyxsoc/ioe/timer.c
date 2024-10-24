#include <am.h>
#include <ysyxsoc.h>

#define CLINT_MTIME_LOW CLINT_MTIME
#define CLINT_MTIME_HITH (CLINT_MTIME + 4)

static uint64_t boot_time = 0;

void __am_timer_init() {
  boot_time = (uint64_t)inl(CLINT_MTIME_HITH) << 32;
  boot_time = boot_time + inl(CLINT_MTIME_LOW);
  boot_time >>= 0;
}

void __am_timer_uptime(AM_TIMER_UPTIME_T *uptime) {
  uptime->us = (uint64_t)inl(CLINT_MTIME_HITH) << 32;
  uptime->us = uptime->us + inl(CLINT_MTIME_LOW);
  uptime->us -= boot_time;
  uptime->us >>= 0;
}

void __am_timer_rtc(AM_TIMER_RTC_T *rtc) {
  rtc->second = 0;
  rtc->minute = 0;
  rtc->hour   = 0;
  rtc->day    = 0;
  rtc->month  = 0;
  rtc->year   = 1900;
}
