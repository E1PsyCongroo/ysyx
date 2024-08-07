#ifndef __SDB_H__
#define __SDB_H__

#include <common.h>

word_t expr(char *e, bool *success);

typedef struct watchpoint WP;

WP* new_wp();
void free_wp(WP *wp);
bool add_wp(char *e);
bool delete_wp(int no);
void watchpoint_display(void);
bool scan_wp(void);

#endif
