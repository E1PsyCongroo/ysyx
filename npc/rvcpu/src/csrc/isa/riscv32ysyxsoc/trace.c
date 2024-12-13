#include <elf.h>
#include <common.h>
#include <memory/paddr.h>
#include <cpu/decode.h>

typedef struct {
  char *fname;
  word_t fsize;
  word_t faddr;
} FunctionTable;

static FunctionTable *func_table = NULL;
static size_t func_table_size = 0;
static char *elf_str = NULL;
static char *current_func = NULL;

char* trace_func(word_t dnpc);
void ftrace(uint32_t instruction, word_t pc, word_t dnpc);

void init_ftrace(const char* elf_file) {
  if (elf_file == NULL) { return; }
  FILE *fp = fopen(elf_file, "rb");
  Assert(fp, "Can not open '%s'", elf_file);

  /* read ELF header */
  Elf32_Ehdr elf_ehdr;
  Assert(fread(&elf_ehdr, sizeof elf_ehdr, 1, fp) != sizeof elf_ehdr, "%s is not a elf file\n", elf_file);
  Assert(*(uint32_t*)elf_ehdr.e_ident == 0x464c457f, "%s is not a elf file\n", elf_file);

  /* read Section header */
  fseek(fp, elf_ehdr.e_shoff, SEEK_SET);
  Elf32_Shdr elf_shdr[elf_ehdr.e_shnum];
  fread(&elf_shdr, elf_ehdr.e_shentsize, elf_ehdr.e_shnum, fp);
  Elf32_Off sym_offset = 0, str_offset = 0;
  Elf32_Word sym_size = 0, str_size = 0;
  for (size_t i = 0; i < elf_ehdr.e_shnum; i++) {
    if (elf_shdr[i].sh_type == SHT_SYMTAB && !sym_offset) {
      sym_offset = elf_shdr[i].sh_offset;
      sym_size = elf_shdr[i].sh_size;
    }
    else if (elf_shdr[i].sh_type == SHT_STRTAB && !str_offset) {
      str_offset = elf_shdr[i].sh_offset;
      str_size = elf_shdr[i].sh_size;
    }
  }

  /* read Symbol table */
  size_t elf_symentnum = sym_size / sizeof(Elf32_Sym);
  Elf32_Sym elf_sym[elf_symentnum];
  fseek(fp, sym_offset, SEEK_SET);
  fread(elf_sym, sym_size, 1, fp);

  /* read String table */
  elf_str = malloc((sizeof *elf_str) * str_size);
  fseek(fp, str_offset, SEEK_SET);
  fread(elf_str, str_size, 1, fp);

  /* generating ftrace table*/
  for (size_t i = 0; i < elf_symentnum; i++) {
    if (ELF32_ST_TYPE(elf_sym[i].st_info) == STT_FUNC) {
      func_table_size++;
    }
  }
  if (!func_table_size) { return; }
  func_table = malloc((sizeof *func_table) * func_table_size);
  FunctionTable *tp = func_table;
  for (size_t i = 0; i < elf_symentnum; i++) {
    if (ELF32_ST_TYPE(elf_sym[i].st_info) == STT_FUNC) {
      tp->faddr = elf_sym[i].st_value;
      tp->fsize = elf_sym[i].st_size;
      tp->fname = &elf_str[elf_sym[i].st_name];
      tp++;
    }
  }
  current_func = trace_func(RESET_VECTOR);
  Log("The elf is %s, function table size = %zd", elf_file, func_table_size);
}

char* trace_func(word_t dnpc) {
  if (func_table == NULL) { return NULL; }
  for (size_t i = 0; i < func_table_size; i++) {
    if (dnpc >= func_table[i].faddr && dnpc < func_table[i].faddr + func_table[i].fsize) {
      return func_table[i].fname;
    }
  }
  return NULL;
}

void ftrace(uint32_t instruction, word_t pc, word_t dnpc) {
  static int num_space = 0;
  char *trace_name = trace_func(dnpc);
  if (trace_name == NULL) { trace_name = "???"; }
#define INSTPAT_INST(s) (instruction)
#define INSTPAT_MATCH(s, ... /* execute body */ ) { \
  __VA_ARGS__ ; \
}
  INSTPAT_START();
  /* function call (jal ra, func | jalr ra, offset(rs1)) */
  INSTPAT("??????? ????? ????? ??? 00001 110?1 11",
    log_write(
      ANSI_FMT(FMT_WORD ": %*scall[%s->%s@" FMT_WORD "]\n", ANSI_FG_YELLOW),
      pc, num_space, "", current_func, trace_name, dnpc
    );
    num_space += 1;
  );
  /* function ret (jalr x0, 0(ra)) */
  INSTPAT("??????? ????? 00001 ??? 00000 11001 11",
    num_space = num_space == 0 ? 0 : num_space - 1;
    log_write(
      ANSI_FMT(FMT_WORD ": %*sret[%s->%s@" FMT_WORD "]\n", ANSI_FG_YELLOW),
      pc, num_space, "", current_func, trace_name, dnpc
    );
  );
  INSTPAT_END();
  current_func = trace_name;
}

void etrace(uint32_t instruction, word_t pc) {
  static int num_space = 0;
#define INSTPAT_INST(s) (instruction)
#define INSTPAT_MATCH(s, ... /* execute body */ ) { \
  __VA_ARGS__ ; \
}
  INSTPAT_START();
  /* ecall */
  INSTPAT("0000000 00000 00000 000 00000 11100 11",
    log_write(
      ANSI_FMT(FMT_WORD ": %*sException throw: " FMT_WORD "\n", ANSI_FG_WHITE),
      pc, num_space, "", cpu.mcause
    );
    num_space += 1;
  );
  /* mret */
  INSTPAT("0011000 00010 00000 000 00000 11100 11",
    num_space = num_space == 0 ? 0 : num_space - 1;
    log_write(
      ANSI_FMT(FMT_WORD ": %*sException return: [%s@" FMT_WORD "]\n", ANSI_FG_WHITE),
      pc, num_space, "", current_func, cpu.mepc
    );

  );
  INSTPAT_END();
}