#include <elf.h>
#include <common.h>

typedef struct {
  char *fname;
  word_t fsize;
  word_t faddr;
} FunctionTable;

static FunctionTable *func_table = NULL;
static size_t func_table_size = 0;
static char *elf_str = NULL;

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
  static char *current_func = "_start";
  static int num_space = 0;
  char *trace_name = trace_func(dnpc);
  uint32_t opcode = BITS(instruction, 6, 0);
  uint32_t rs1 = BITS(instruction, 19, 15);
  uint32_t rd = BITS(instruction, 11, 7);
  const uint32_t jal_opcode = 0b1101111;
  const uint32_t jalr_opcode = 0b1100111;
  if (trace_name == NULL) { trace_name = "???"; }
  if (((opcode == jal_opcode) | (opcode == jalr_opcode)) && rd == 1) {
    /* function call (jal ra, func | jalr ra, offset(rs1)) */
    log_write(
      ANSI_FMT(FMT_WORD ": %*scall[%s->%s@" FMT_WORD "]\n", ANSI_FG_YELLOW),
      pc, num_space, "", current_func, trace_name, dnpc
    );
    num_space += 1;
  }
  else if ((opcode == jalr_opcode) && (rs1 == 1) && (rd == 0)) {
    /* function ret (jalr x0, 0(ra)) */
    num_space = num_space == 0 ? 0 : num_space - 1;
    log_write(
      ANSI_FMT(FMT_WORD ": %*sret[%s->%s@" FMT_WORD "]\n", ANSI_FG_YELLOW),
      pc, num_space, "", current_func, trace_name, dnpc
    );
  }
  current_func = trace_name;
}