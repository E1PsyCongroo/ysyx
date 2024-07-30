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

/* We use the POSIX regex functions to process regular expressions.
 * Type 'man regex' for more information about POSIX regex functions.
 */
#include <regex.h>

enum {
  TK_NOTYPE = 256, TK_EQ,
  TK_DEC, TK_HEX,
  TK_NEQ, TK_AND,
  TK_NEG, TK_DEREF,
  TK_REG, TK_OR,
  /* TODO: Add more token types */

};

static struct rule {
  const char *regex;
  int token_type;
} rules[] = {

  /* TODO: Add more rules.
   * Pay attention to the precedence level of different rules.
   */

  {" +", TK_NOTYPE},                      // spaces
  {"\\+", '+'},                           // plus
  {"==", TK_EQ},                          // equal
  {"!=", TK_NEQ},                         // not equal
  {"&&", TK_AND},                         // and
  {"\\|\\|", TK_OR},                      // or
  {"-", '-'},                             // minus
  {"\\*", '*'},                           // mul
  {"/", '/'},                             // div
  {"\\(", '('},                           // left bracket
  {"\\)", ')'},                           // right bracket
  {"0[xX][0-9A-Fa-f]+", TK_HEX},          // hex number
  {"[0-9]+", TK_DEC},                     // decimal number
  {"\\$[$a-zA-Z]+[a-zA-Z0-9]*", TK_REG},  // register
};

#define NR_REGEX ARRLEN(rules)

static regex_t re[NR_REGEX] = {};

/* Rules are used for many times.
 * Therefore we compile them only once before any usage.
 */
void init_regex() {
  int i;
  char error_msg[128];
  int ret;

  for (i = 0; i < NR_REGEX; i ++) {
    ret = regcomp(&re[i], rules[i].regex, REG_EXTENDED);
    if (ret != 0) {
      regerror(ret, &re[i], error_msg, 128);
      panic("regex compilation failed: %s\n%s", error_msg, rules[i].regex);
    }
  }
}

typedef struct token {
  int type;
  char str[32];
} Token;

static Token tokens[32] __attribute__((used)) = {};
static int nr_token __attribute__((used))  = 0;

static bool make_token(char *e) {
  int position = 0;
  int i;
  regmatch_t pmatch;

  nr_token = 0;

  while (e[position] != '\0') {
    /* Try all rules one by one. */
    for (i = 0; i < NR_REGEX; i ++) {
      if (regexec(&re[i], e + position, 1, &pmatch, 0) == 0 && pmatch.rm_so == 0) {
        char *substr_start = e + position;
        int substr_len = pmatch.rm_eo;

        Log("match rules[%d] = \"%s\" at position %d with len %d: %.*s",
            i, rules[i].regex, position, substr_len, substr_len, substr_start);

        position += substr_len;

        /* TODO: Now a new token is recognized with rules[i]. Add codes
         * to record the token in the array `tokens'. For certain types
         * of tokens, some extra actions should be performed.
         */

        tokens[nr_token].type = rules[i].token_type;
        switch (rules[i].token_type) {
          case TK_NOTYPE:
            break;
          case '+': case TK_EQ: case '-': case '*':
          case '/': case '(': case ')': case TK_AND:
          case TK_OR: case TK_NEQ:
            nr_token++;
            break;
          case TK_DEC: case TK_HEX:
            Assert(substr_len < 32, "token \"%.*s\" length overflow", substr_len, substr_start);
            strncpy(tokens[nr_token].str, substr_start, substr_len);
            tokens[nr_token++].str[substr_len] = '\0';
            break;
          default:
            panic("unknown token: %.*s", substr_len, substr_start);
        }

        break;
      }
    }

    if (i == NR_REGEX) {
      printf("no match at position %d\n%s\n%*.s^\n", position, e, position, "");
      return false;
    }
  }

  return true;
}

static bool check_parentheses(int p, int q) {
  if (tokens[p].type != '(' || tokens[q].type != ')') {
    return false;
  }
  int unmatch = 0;
  for (int i = p + 1; i < q; i++) {
    if (tokens[i].type == '(') { unmatch++; }
    else if (tokens[i].type == ')') {
      if (unmatch == 0) { return false; }
      unmatch--;
    }
  }
  return unmatch == 0;
}

static int op_priority[] = {
  [TK_OR] = 12,
  [TK_AND] = 11,
  [TK_EQ] = 7, [TK_NEG] = 7,
  ['+'] = 4, ['-'] = 4,
  ['*'] = 3, ['/'] = 3,
  [TK_NEG] = 2, [TK_DEREF] = 2,
};

static int find_mainop(int p, int q) {
  int unmatch = 0;
  int position = p;
  int cur_priority = 0;
  for (int i = p; i <= q; i++) {
    switch (tokens[i].type)
    {
    case TK_NOTYPE: case TK_HEX: case TK_DEC: case TK_REG:
    case TK_NEG: case TK_DEREF:
      break;
    case '+': case TK_EQ: case '-': case '*':
    case '/': case TK_NEQ: case TK_AND: case TK_OR:
      if (unmatch == 0 && op_priority[tokens[i].type] >= cur_priority) {
        position = i;
        cur_priority = op_priority[tokens[i].type];
      }
      break;
    case '(':
      unmatch++;
      break;
    case ')':
      unmatch--;
      break;
    default: TODO();
    }
  }
  return position;
}

word_t eval(int p, int q, bool *success) {
  word_t result = 0;
  if (!*success) { return 0; }
  if (p > q) {
    *success = false;
  }
  else if (p == q) {
    switch (tokens[p].type) {
    case TK_DEC:
      Assert(
        sscanf(tokens[p].str, FMT_WORD_DEC, &result),
        "unknown token: \"%s\"", tokens[p].str
      );
      break;
    case TK_HEX:
      Assert(
        sscanf(tokens[p].str, FMT_WORD_HEX, &result),
        "unknown token: \"%s\"", tokens[p].str
      );
      break;
    case TK_REG:
      result = isa_reg_str2val(tokens[p].str + 1, success);
      break;
    default:
      *success = false;
    }
  }
  else if (p == q - 1) {
    word_t val = eval(p + 1, q, success);
    switch (tokens[p].type) {
    case TK_NEG:
      result = -val;
      break;
    case TK_DEREF:
      word_t paddr_read(paddr_t addr, int len);
      result = paddr_read(val, 4);
      break;
    default: panic("unknown unary op: %d", tokens[p].type);
    }
  }
  else if (check_parentheses(p, q)) {
    result = eval(p + 1, q - 1, success);
  }
  else {
    int mainop_position = find_mainop(p, q);
    word_t val1 = eval(p, mainop_position - 1, success);
    word_t val2 = eval(mainop_position + 1, q, success);
    switch (tokens[mainop_position].type) {
    case '+':
      result = val1 + val2;
      break;
    case '-':
      result = val1 - val2;
      break;
    case '*':
      result = val1 * val2;
      break;
    case '/':
      Assert(val2 != 0, "divide by zero");
      result = val1 / val2;
      break;
    default: panic("unknown op: %d", tokens[mainop_position].type);
    }
  }
  return result;
}

static bool is_op(int index) {
  switch (index) {
  case '+': case '-': case '*': case '/':
  case TK_OR: case TK_EQ: case TK_NEQ: case TK_AND:
    return true;
  }
  return false;
}

static void mark_unary(void) {
  for (int i = 0; i < nr_token; i++) {
    if (i == 0 || is_op(i - 1)) {
      switch (tokens[i].type)
      {
      case '*':
        tokens[i].type = TK_DEREF;
        break;
      case '-':
        tokens[i].type = TK_NEG;
        break;
      default: break;
      }
    }
  }
}

word_t expr(char *e, bool *success) {
  *success = true;
  if (!make_token(e)) {
    *success = false;
    return 0;
  }

  /* TODO: Insert codes to evaluate the expression. */
  mark_unary();
  return eval(0, nr_token-1, success);
}
