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

#include <isa.h>
#include <cpu/cpu.h>
#include <readline/readline.h>
#include <readline/history.h>
#include "sdb.h"

static int is_batch_mode = false;

void init_regex();
void init_wp_pool();

/* We use the `readline' library to provide more flexibility to read from stdin. */
static char* rl_gets() {
  static char *line_read = NULL;

  if (line_read) {
    free(line_read);
    line_read = NULL;
  }

  line_read = readline("(nemu) ");

  if (line_read && *line_read) {
    add_history(line_read);
  }

  return line_read;
}

static int cmd_help(char *args);
static int cmd_c(char *args);
static int cmd_q(char *args);
static int cmd_si(char *args);
static int cmd_info(char *args);
static int cmd_x(char *args);
static int cmd_p(char *args);
static int cmd_w(char *args);
static int cmd_d(char *args);
static int cmd_test(char *args);

static struct {
  const char *name;
  const char *description;
  int (*handler) (char *);
} cmd_table [] = {
  { "help", "Display information about all supported commands", cmd_help },
  { "c", "Continue the execution of the program", cmd_c },
  { "q", "Exit NEMU", cmd_q },

  /* TODO: Add more commands */
  { "si", "Lets the program pause after executing N instructions using single step execution, When N is not given, the default is 1", cmd_si },
  { "info", "Print register status/watchpoint information", cmd_info },
  { "x", "Finds the value of the expression EXPR, uses the result as the starting memory address, and outputs consecutive N 4 bytes in hexadecimal", cmd_x },
  { "p", "Find the value of the expression EXPR, for EXPR supported operations", cmd_p },
  { "w", "Suspend program execution when the value of expression EXPR changes", cmd_w },
  { "d", "Deletes the watchpoint with ID N", cmd_d },
  { "test", "build-in test", cmd_test },

};

#define NR_CMD ARRLEN(cmd_table)

static int cmd_help(char *args) {
  /* extract the first argument */
  char *arg = strtok(NULL, " ");
  int i;

  if (arg == NULL) {
    /* no argument given */
    for (i = 0; i < NR_CMD; i ++) {
      printf("%s - %s\n", cmd_table[i].name, cmd_table[i].description);
    }
  }
  else {
    for (i = 0; i < NR_CMD; i ++) {
      if (strcmp(arg, cmd_table[i].name) == 0) {
        printf("%s - %s\n", cmd_table[i].name, cmd_table[i].description);
        return 0;
      }
    }
    printf("Unknown command '%s'\n", arg);
  }
  return 0;
}

static int cmd_c(char *args) {
  cpu_exec(-1);
  return 0;
}

static int cmd_q(char *args) {
  nemu_state.state = NEMU_QUIT;
  return -1;
}

static int cmd_si(char *args) {
  char *arg = strtok(NULL, " ");
  uint64_t exec_count = 1;
  if (arg) {
    Assert(sscanf(arg, "%" PRIu64, &exec_count), "cmd si: invalid arg \"%s\"", arg);
  }
  cpu_exec(exec_count);
  return 0;
}

static int cmd_info(char *args) {
  char *arg = strtok(NULL, " ");
  Assert(arg, "cmd info: need arg r/w");
  if (!strcasecmp(arg, "r")) {
    isa_reg_display();
  }
  else if (!strcasecmp(arg, "w")) {
    watchpoint_display();
  }
  else {
    panic("cmd info: invalid arg \"%s\"", arg);
  }
  return 0;
}

static int cmd_x(char *args) {
  char *arg_watch_count = strtok(NULL, " ");
  char *arg_addr_expr = strtok(NULL, "");
  Assert(arg_watch_count && arg_addr_expr, "cmd x: need args [N] [EXPR]");

  uint64_t watch_count;
  Assert(sscanf(arg_watch_count, "%" PRIu64, &watch_count), "cmd si: invalid arg \"%s\"", arg_watch_count);
  bool success = true;
  paddr_t addr = expr(arg_addr_expr, &success);
  Assert(success, "invalid expression: %s\n", arg_addr_expr);

  for (uint64_t i = 0; i < watch_count; i++) {
    if (i % 4 == 0) {
      printf(ANSI_FMT(FMT_PADDR, ANSI_FG_BLUE)": ", addr);
    }
    word_t paddr_read(paddr_t addr, int len);
    printf("0x%08x%7s", paddr_read(addr, 4), "");
    addr += 4;
    if (i % 4 == 3 || i == watch_count - 1) {
      putchar('\n');
    }
  }
  return 0;
}

static int cmd_p(char *args) {
  char *arg = strtok(NULL, "");
  Assert(arg, "cmd p: need arg expr");
  bool success = true;
  word_t result = expr(arg, &success);
  if (success) {
    printf(FMT_WORD " " FMT_WORD_DEC "\n", result, result);
  }
  else {
    printf(ANSI_FMT("invalid expresion\n", ANSI_FG_RED));
  }
  return 0;
}

static int cmd_w(char *args) {
  char *expr = strtok(NULL, "");
  Assert(expr, "cmd w: need arg expr");
  if (!add_wp(expr)) {
    printf(ANSI_FMT("invalid expresion\n", ANSI_FG_RED));
  }
  return 0;
}

static int cmd_d(char *args) {
  char *arg = strtok(NULL, "");
  Assert(arg, "cmd d: need arg NO");
  int no;
  Assert(sscanf(arg, "%d", &no), "cmd d: invalid arg \"%s\"", arg);
  if (!delete_wp(no)) {
    printf(ANSI_FMT("unknown watchpoint \"%d\"\n", ANSI_FG_RED), no);
  }
  return 0;
}

static int cmd_test(char *args) {
  char *arg = strtok(NULL, "");
  char buf[65536];
  int pass = 0, total = 0;
  FILE *fp = fopen(arg, "r");
  Assert(fp != NULL, "open \"%s\" fail", arg);
  while (fgets(buf, 65536, fp)){
    buf[strlen(buf)-1] = '\0';
    total++;
    char* argResult = strtok(buf, " ");
    char* argExpr = strtok(NULL, "");
    uint32_t expected, result;
    bool success = true;
    sscanf(argResult, "%" PRIu32, &expected);
    result = (uint32_t)expr(argExpr, &success);
    if (success && result == expected) { pass++; }
    else {
      printf("FAIL: expr:%s, expected:%u, result:%u\n", argExpr, expected, result);
      fflush(stdout);
      assert(0);
    }
  }
  fclose(fp);
  printf("PASS: %d/%d\n", pass, total);
  return 0;
}

void sdb_set_batch_mode() {
  is_batch_mode = true;
}

void sdb_mainloop() {
  if (is_batch_mode) {
    cmd_c(NULL);
    return;
  }

  for (char *str; (str = rl_gets()) != NULL; ) {
    char *str_end = str + strlen(str);

    /* extract the first token as the command */
    char *cmd = strtok(str, " ");
    if (cmd == NULL) { continue; }

    /* treat the remaining string as the arguments,
     * which may need further parsing
     */
    char *args = cmd + strlen(cmd) + 1;
    if (args >= str_end) {
      args = NULL;
    }

#ifdef CONFIG_DEVICE
    extern void sdl_clear_event_queue();
    sdl_clear_event_queue();
#endif

    int i;
    for (i = 0; i < NR_CMD; i ++) {
      if (strcmp(cmd, cmd_table[i].name) == 0) {
        if (cmd_table[i].handler(args) < 0) { return; }
        break;
      }
    }

    if (i == NR_CMD) { printf("Unknown command '%s'\n", cmd); }
  }
}

void init_sdb() {
  /* Compile the regular expressions. */
  init_regex();

  /* Initialize the watchpoint pool. */
  init_wp_pool();
}
