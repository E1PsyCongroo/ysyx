#include "common.h"
#include "sdb.h"

#define NR_WP 32

typedef struct watchpoint {
  int NO;
  struct watchpoint *next;

  /* TODO: Add more members if necessary */
  char *expr;
  word_t pre_val;

} WP;

static WP wp_pool[NR_WP] = {};
static WP *head = NULL, *free_ = NULL;

void init_wp_pool() {
  int i;
  for (i = 0; i < NR_WP; i ++) {
    wp_pool[i].NO = i;
    wp_pool[i].next = (i == NR_WP - 1 ? NULL : &wp_pool[i + 1]);
  }

  head = NULL;
  free_ = wp_pool;
}

/* TODO: Implement the functionality of watchpoint */

WP* new_wp() {
  Assert(free_, "no available watchpoints");
  WP *new_watchpoint = free_;
  free_ = free_->next;
  return new_watchpoint;
}

void free_wp(WP *wp) {
  free(wp->expr);
  wp->expr = NULL;
  if (wp == head) {
    head = wp->next;
  }
  else {
    WP *previous = head;
    while (previous->next == wp) {
      previous = previous->next;
    }
    previous->next = wp->next;
  }
  wp->next = free_;
  free_ = wp;
}

bool add_wp(char *e) {
  bool success;
  word_t pre_val = expr(e, &success);
  if (!success) return success;

  WP *new_watchpoint = new_wp();
  int length = strlen(e);
  new_watchpoint->expr = malloc(length + 1);
  strncpy(new_watchpoint->expr, e, length + 1);
  new_watchpoint->pre_val = pre_val;
  new_watchpoint->next = head;
  head = new_watchpoint;
  return success;
}

bool delete_wp(int no) {
  WP *wp = head;
  while (wp) {
    if (wp->NO == no) {
      free_wp(wp);
      return true;
    }
    wp = wp->next;
  }
  return false;
}

void watchpoint_display(void) {
  if (!head) {
    printf("No watchpoints\n");
  }
  else {
    printf("Num What\n");
    for (WP *t = head; t != NULL; t = t->next) {
      printf("%-3d %s\n", t->NO, t->expr);
    }
  }
}

bool scan_wp(void) {
  WP *wp = head;
  bool change = false, success;
  while (wp) {
    word_t new_val = expr(wp->expr, &success);
    if (new_val != wp->pre_val) {
      printf("watchpoint %d: %s\n", wp->NO, wp->expr);
      printf("Old value: " FMT_WORD FMT_WORD_DEC "\nNew value: " FMT_WORD FMT_WORD_DEC"\n",
            wp->pre_val, wp->pre_val, new_val, new_val);
      wp->pre_val = new_val;
      change = true;
    }
    wp = wp->next;
  }
  return change;
}
