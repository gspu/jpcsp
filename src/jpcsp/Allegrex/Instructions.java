
/* this is an auto-generated file from Allegrex.isa file */
/* This file is part of jpcsp.
Jpcsp is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

Jpcsp is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with Jpcsp.  If not, see <http://www.gnu.org/licenses/>.
 */
package jpcsp.Allegrex;

import jpcsp.Processor;

import static jpcsp.Debugger.DisassemblerModule.DisHelper.*;

/**
 *
 * @author hli
 */
public class Instructions {

    public static abstract class Instruction {

        public abstract void interpret(Processor processor, int insn);

        public abstract void compile(Processor processor, int insn);

        public abstract String disasm(int address, int insn);
    }
    public static final Instruction NOP = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
        }

        @Override
        public void compile(Processor processor, int insn) {
        }

        @Override
        public String disasm(int address, int insn) {


            return "nop";
        }
    };
    public static final Instruction CACHE = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int imm16 = (insn >> 0) & 65535;
            int rt = (insn >> 16) & 31;
            int rs = (insn >> 21) & 31;



        }

        @Override
        public void compile(Processor processor, int insn) {
            int imm16 = (insn >> 0) & 65535;
            int rt = (insn >> 16) & 31;
            int rs = (insn >> 21) & 31;



        }

        @Override
        public String disasm(int address, int insn) {
            int imm16 = (insn >> 0) & 65535;
            int rt = (insn >> 16) & 31;
            int rs = (insn >> 21) & 31;


            return Dis_CCIMMRS("cache", rt, signExtend(imm16), rs);
        }
    };
    public static final Instruction SYSCALL = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int imm20 = (insn >> 6) & 1048575;




        }

        @Override
        public void compile(Processor processor, int insn) {
            int imm20 = (insn >> 6) & 1048575;




        }

        @Override
        public String disasm(int address, int insn) {
            int imm20 = (insn >> 6) & 1048575;


            return Dis_Syscall(imm20);
        }
    };
    public static final Instruction ERET = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
        }

        @Override
        public void compile(Processor processor, int insn) {
        }

        @Override
        public String disasm(int address, int insn) {


            return "eret";
        }
    };
    public static final Instruction BREAK = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int imm20 = (insn >> 6) & 1048575;




        }

        @Override
        public void compile(Processor processor, int insn) {
            int imm20 = (insn >> 6) & 1048575;




        }

        @Override
        public String disasm(int address, int insn) {
            int imm20 = (insn >> 6) & 1048575;


            return Dis_Break(imm20);
        }
    };
    public static final Instruction SYNC = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
        }

        @Override
        public void compile(Processor processor, int insn) {
        }

        @Override
        public String disasm(int address, int insn) {


            return "sync";
        }
    };
    public static final Instruction ADD = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int rd = (insn >> 11) & 31;
            int rt = (insn >> 16) & 31;
            int rs = (insn >> 21) & 31;



            // just ignore overflow exception as it is useless
            if (rd != 0) {
                processor.gpr[rd] = processor.gpr[rs] + processor.gpr[rt];
            }

        }

        @Override
        public void compile(Processor processor, int insn) {
            int rd = (insn >> 11) & 31;
            int rt = (insn >> 16) & 31;
            int rs = (insn >> 21) & 31;



            if (rd == 0) {
                return;
            }
            if (processor.tracked_gpr[rs].fixed && processor.tracked_gpr[rt].fixed) {
                processor.fix_gpr(rd, processor.gpr[rs] + processor.gpr[rt]);
            } else {
                processor.load_gpr(rs, false);
                processor.load_gpr(rt, false);
                processor.alter_gpr(rd);
                processor.current_bb.emit(
                        processor.get_gpr(rd) +
                        " = (" +
                        processor.get_gpr(rs) +
                        " + " +
                        processor.get_gpr(rt) +
                        ");");
            }

        }

        @Override
        public String disasm(int address, int insn) {
            int rd = (insn >> 11) & 31;
            int rt = (insn >> 16) & 31;
            int rs = (insn >> 21) & 31;


            return Dis_RDRSRT("add", rd, rs, rt);
        }
    };
    public static final Instruction ADDU = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int rd = (insn >> 11) & 31;
            int rt = (insn >> 16) & 31;
            int rs = (insn >> 21) & 31;



            if (rd != 0) {
                processor.gpr[rd] = processor.gpr[rs] + processor.gpr[rt];
            }

        }

        @Override
        public void compile(Processor processor, int insn) {
            int rd = (insn >> 11) & 31;
            int rt = (insn >> 16) & 31;
            int rs = (insn >> 21) & 31;



            if (rd == 0) {
                return;
            }
            if (processor.tracked_gpr[rs].fixed && processor.tracked_gpr[rt].fixed) {
                processor.fix_gpr(rd, processor.gpr[rs] + processor.gpr[rt]);
            } else {
                processor.load_gpr(rs, false);
                processor.load_gpr(rt, false);
                processor.alter_gpr(rd);
                processor.current_bb.emit(
                        processor.get_gpr(rd) +
                        " = (" +
                        processor.get_gpr(rs) +
                        " + " +
                        processor.get_gpr(rt) +
                        ");");
            }

        }

        @Override
        public String disasm(int address, int insn) {
            int rd = (insn >> 11) & 31;
            int rt = (insn >> 16) & 31;
            int rs = (insn >> 21) & 31;


            return Dis_RDRSRT("addu", rd, rs, rt);
        }
    };
    public static final Instruction ADDI = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int imm16 = (insn >> 0) & 65535;
            int rt = (insn >> 16) & 31;
            int rs = (insn >> 21) & 31;



            // just ignore overflow exception as it is useless
            if (rt != 0) {
                processor.gpr[rt] = processor.gpr[rs] + Processor.signExtend(imm16);
            }

        }

        @Override
        public void compile(Processor processor, int insn) {
            int imm16 = (insn >> 0) & 65535;
            int rt = (insn >> 16) & 31;
            int rs = (insn >> 21) & 31;



            if (rt == 0) {
                return;
            }
            if (processor.tracked_gpr[rs].fixed) {
                processor.fix_gpr(rt, processor.gpr[rs] + Processor.signExtend(imm16));
            } else {
                processor.load_gpr(rs, false);
                processor.alter_gpr(rt);
                processor.current_bb.emit(
                        processor.get_gpr(rt) +
                        " = (" +
                        processor.get_gpr(rs) +
                        " + (" +
                        Processor.signExtend(imm16) +
                        "));");
            }

        }

        @Override
        public String disasm(int address, int insn) {
            int imm16 = (insn >> 0) & 65535;
            int rt = (insn >> 16) & 31;
            int rs = (insn >> 21) & 31;


            return Dis_RTRSIMM("addi", rt, rs, signExtend(imm16));
        }
    };
    public static final Instruction ADDIU = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int imm16 = (insn >> 0) & 65535;
            int rt = (insn >> 16) & 31;
            int rs = (insn >> 21) & 31;



            if (rt != 0) {
                processor.gpr[rt] = processor.gpr[rs] + Processor.signExtend(imm16);
            }

        }

        @Override
        public void compile(Processor processor, int insn) {
            int imm16 = (insn >> 0) & 65535;
            int rt = (insn >> 16) & 31;
            int rs = (insn >> 21) & 31;



            if (rt == 0) {
                return;
            }
            if (processor.tracked_gpr[rs].fixed) {
                processor.fix_gpr(rt, processor.gpr[rs] + Processor.signExtend(imm16));
            } else {
                processor.load_gpr(rs, false);
                processor.alter_gpr(rt);
                processor.current_bb.emit(
                        processor.get_gpr(rt) +
                        " = (" +
                        processor.get_gpr(rs) +
                        " + (" +
                        Processor.signExtend(imm16) +
                        "));");
            }

        }

        @Override
        public String disasm(int address, int insn) {
            int imm16 = (insn >> 0) & 65535;
            int rt = (insn >> 16) & 31;
            int rs = (insn >> 21) & 31;


            return Dis_RTRSIMM("addiu", rt, rs, signExtend(imm16));
        }
    };
    public static final Instruction AND = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int rd = (insn >> 11) & 31;
            int rt = (insn >> 16) & 31;
            int rs = (insn >> 21) & 31;



            if (rd != 0) {
                processor.gpr[rd] = processor.gpr[rs] & processor.gpr[rt];
            }

        }

        @Override
        public void compile(Processor processor, int insn) {
            int rd = (insn >> 11) & 31;
            int rt = (insn >> 16) & 31;
            int rs = (insn >> 21) & 31;



            if (rd == 0) {
                return;
            }
            if (processor.tracked_gpr[rs].fixed && processor.tracked_gpr[rt].fixed) {
                processor.fix_gpr(rd, processor.gpr[rs] & processor.gpr[rt]);
            } else {
                processor.load_gpr(rs, false);
                processor.load_gpr(rt, false);
                processor.alter_gpr(rd);
                processor.current_bb.emit(
                        processor.get_gpr(rd) +
                        " = (" +
                        processor.get_gpr(rs) +
                        " & " +
                        processor.get_gpr(rt) +
                        ");");
            }

        }

        @Override
        public String disasm(int address, int insn) {
            int rd = (insn >> 11) & 31;
            int rt = (insn >> 16) & 31;
            int rs = (insn >> 21) & 31;


            return Dis_RDRSRT("and", rd, rs, rt);
        }
    };
    public static final Instruction ANDI = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int imm16 = (insn >> 0) & 65535;
            int rt = (insn >> 16) & 31;
            int rs = (insn >> 21) & 31;



            if (rt != 0) {
                processor.gpr[rt] = processor.gpr[rs] & Processor.zeroExtend(imm16);
            }

        }

        @Override
        public void compile(Processor processor, int insn) {
            int imm16 = (insn >> 0) & 65535;
            int rt = (insn >> 16) & 31;
            int rs = (insn >> 21) & 31;



            if (rt == 0) {
                return;
            }
            if (processor.tracked_gpr[rs].fixed) {
                processor.fix_gpr(rt, processor.gpr[rs] & Processor.zeroExtend(imm16));
            } else {
                processor.load_gpr(rs, false);
                processor.alter_gpr(rt);
                processor.current_bb.emit(
                        processor.get_gpr(rt) +
                        " = (" +
                        processor.get_gpr(rs) +
                        " & (" +
                        Processor.zeroExtend(imm16) +
                        "));");
            }

        }

        @Override
        public String disasm(int address, int insn) {
            int imm16 = (insn >> 0) & 65535;
            int rt = (insn >> 16) & 31;
            int rs = (insn >> 21) & 31;


            return Dis_RTRSIMM("andi", rt, rs, zeroExtend(imm16));
        }
    };
    public static final Instruction NOR = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int rd = (insn >> 11) & 31;
            int rt = (insn >> 16) & 31;
            int rs = (insn >> 21) & 31;



            if (rd != 0) {
                processor.gpr[rd] = ~(processor.gpr[rs] | processor.gpr[rt]);
            }

        }

        @Override
        public void compile(Processor processor, int insn) {
            int rd = (insn >> 11) & 31;
            int rt = (insn >> 16) & 31;
            int rs = (insn >> 21) & 31;



            if (rd == 0) {
                return;
            }
            if (processor.tracked_gpr[rs].fixed && processor.tracked_gpr[rt].fixed) {
                processor.fix_gpr(rd, ~(processor.gpr[rs] | processor.gpr[rt]));
            } else {
                processor.load_gpr(rs, false);
                processor.load_gpr(rt, false);
                processor.alter_gpr(rd);
                processor.current_bb.emit(
                        processor.get_gpr(rd) +
                        " = ~(" +
                        processor.get_gpr(rs) +
                        " | " +
                        processor.get_gpr(rt) +
                        ");");
            }

        }

        @Override
        public String disasm(int address, int insn) {
            int rd = (insn >> 11) & 31;
            int rt = (insn >> 16) & 31;
            int rs = (insn >> 21) & 31;


            return Dis_RDRSRT("nor", rd, rs, rt);
        }
    };
    public static final Instruction OR = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int rd = (insn >> 11) & 31;
            int rt = (insn >> 16) & 31;
            int rs = (insn >> 21) & 31;



            if (rd != 0) {
                processor.gpr[rd] = processor.gpr[rs] | processor.gpr[rt];
            }

        }

        @Override
        public void compile(Processor processor, int insn) {
            int rd = (insn >> 11) & 31;
            int rt = (insn >> 16) & 31;
            int rs = (insn >> 21) & 31;



            if (rd == 0) {
                return;
            }
            if (processor.tracked_gpr[rs].fixed && processor.tracked_gpr[rt].fixed) {
                processor.fix_gpr(rd, processor.gpr[rs] | processor.gpr[rt]);
            } else {
                processor.load_gpr(rs, false);
                processor.load_gpr(rt, false);
                processor.alter_gpr(rd);
                processor.current_bb.emit(
                        processor.get_gpr(rd) +
                        " = (" +
                        processor.get_gpr(rs) +
                        " | " +
                        processor.get_gpr(rt) +
                        ");");
            }

        }

        @Override
        public String disasm(int address, int insn) {
            int rd = (insn >> 11) & 31;
            int rt = (insn >> 16) & 31;
            int rs = (insn >> 21) & 31;


            return Dis_RDRSRT("or", rd, rs, rt);
        }
    };
    public static final Instruction ORI = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int imm16 = (insn >> 0) & 65535;
            int rt = (insn >> 16) & 31;
            int rs = (insn >> 21) & 31;



            if (rt != 0) {
                processor.gpr[rt] = processor.gpr[rs] | Processor.zeroExtend(imm16);
            }

        }

        @Override
        public void compile(Processor processor, int insn) {
            int imm16 = (insn >> 0) & 65535;
            int rt = (insn >> 16) & 31;
            int rs = (insn >> 21) & 31;



            if (rt == 0) {
                return;
            }
            if (processor.tracked_gpr[rs].fixed) {
                processor.fix_gpr(rt, processor.gpr[rs] | Processor.zeroExtend(imm16));
            } else {
                processor.load_gpr(rs, false);
                processor.alter_gpr(rt);
                processor.current_bb.emit(
                        processor.get_gpr(rt) +
                        " = (" +
                        processor.get_gpr(rs) +
                        " | (" +
                        Processor.zeroExtend(imm16) +
                        "));");
            }

        }

        @Override
        public String disasm(int address, int insn) {
            int imm16 = (insn >> 0) & 65535;
            int rt = (insn >> 16) & 31;
            int rs = (insn >> 21) & 31;


            return Dis_RTRSIMM("ori", rt, rs, zeroExtend(imm16));
        }
    };
    public static final Instruction XOR = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int rd = (insn >> 11) & 31;
            int rt = (insn >> 16) & 31;
            int rs = (insn >> 21) & 31;



            if (rd != 0) {
                processor.gpr[rd] = processor.gpr[rs] ^ processor.gpr[rt];
            }

        }

        @Override
        public void compile(Processor processor, int insn) {
            int rd = (insn >> 11) & 31;
            int rt = (insn >> 16) & 31;
            int rs = (insn >> 21) & 31;



            if (rd == 0) {
                return;
            }
            if (processor.tracked_gpr[rs].fixed && processor.tracked_gpr[rt].fixed) {
                processor.fix_gpr(rd, processor.gpr[rs] ^ processor.gpr[rt]);
            } else {
                processor.load_gpr(rs, false);
                processor.load_gpr(rt, false);
                processor.alter_gpr(rd);
                processor.current_bb.emit(
                        processor.get_gpr(rd) +
                        " = (" +
                        processor.get_gpr(rs) +
                        " ^ " +
                        processor.get_gpr(rt) +
                        ");");
            }

        }

        @Override
        public String disasm(int address, int insn) {
            int rd = (insn >> 11) & 31;
            int rt = (insn >> 16) & 31;
            int rs = (insn >> 21) & 31;


            return Dis_RDRSRT("xor", rd, rs, rt);
        }
    };
    public static final Instruction XORI = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int imm16 = (insn >> 0) & 65535;
            int rt = (insn >> 16) & 31;
            int rs = (insn >> 21) & 31;



            if (rt != 0) {
                processor.gpr[rt] = processor.gpr[rs] ^ Processor.zeroExtend(imm16);
            }

        }

        @Override
        public void compile(Processor processor, int insn) {
            int imm16 = (insn >> 0) & 65535;
            int rt = (insn >> 16) & 31;
            int rs = (insn >> 21) & 31;



            if (rt == 0) {
                return;
            }
            if (processor.tracked_gpr[rs].fixed) {
                processor.fix_gpr(rt, processor.gpr[rs] ^ Processor.zeroExtend(imm16));
            } else {
                processor.load_gpr(rs, false);
                processor.alter_gpr(rt);
                processor.current_bb.emit(
                        processor.get_gpr(rt) +
                        " = (" +
                        processor.get_gpr(rs) +
                        " ^ (" +
                        Processor.zeroExtend(imm16) +
                        "));");
            }

        }

        @Override
        public String disasm(int address, int insn) {
            int imm16 = (insn >> 0) & 65535;
            int rt = (insn >> 16) & 31;
            int rs = (insn >> 21) & 31;


            return Dis_RTRSIMM("xori", rt, rs, zeroExtend(imm16));
        }
    };
    public static final Instruction SLL = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int sa = (insn >> 6) & 31;
            int rd = (insn >> 11) & 31;
            int rt = (insn >> 16) & 31;



            if (rd != 0) {
                processor.gpr[rd] = (processor.gpr[rt] << sa);
            }

        }

        @Override
        public void compile(Processor processor, int insn) {
            int sa = (insn >> 6) & 31;
            int rd = (insn >> 11) & 31;
            int rt = (insn >> 16) & 31;



            if (rd == 0) {
                return;
            }
            if (processor.tracked_gpr[rt].fixed) {
                processor.fix_gpr(rd, (processor.gpr[rt] << sa));
            } else {
                processor.load_gpr(rt, false);
                processor.alter_gpr(rd);
                processor.current_bb.emit(
                        processor.get_gpr(rd) +
                        " = (" +
                        processor.get_gpr(rt) +
                        " << " +
                        sa +
                        ");");
            }

        }

        @Override
        public String disasm(int address, int insn) {
            int sa = (insn >> 6) & 31;
            int rd = (insn >> 11) & 31;
            int rt = (insn >> 16) & 31;


            return Dis_RDRTSA("sll", rd, rt, sa);
        }
    };
    public static final Instruction SLLV = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int rd = (insn >> 11) & 31;
            int rt = (insn >> 16) & 31;
            int rs = (insn >> 21) & 31;



            if (rd != 0) {
                processor.gpr[rd] = processor.gpr[rt] << (processor.gpr[rs] & 31);
            }

        }

        @Override
        public void compile(Processor processor, int insn) {
            int rd = (insn >> 11) & 31;
            int rt = (insn >> 16) & 31;
            int rs = (insn >> 21) & 31;



            if (rd == 0) {
                return;
            }
            if (processor.tracked_gpr[rs].fixed && processor.tracked_gpr[rt].fixed) {
                processor.fix_gpr(rd, processor.gpr[rt] << (processor.gpr[rs] & 31));
            } else {
                processor.load_gpr(rs, false);
                processor.load_gpr(rt, false);
                processor.alter_gpr(rd);
                processor.current_bb.emit(
                        processor.get_gpr(rd) +
                        " = (" +
                        processor.get_gpr(rs) +
                        " << (" +
                        processor.get_gpr(rt) +
                        "&31));");
            }

        }

        @Override
        public String disasm(int address, int insn) {
            int rd = (insn >> 11) & 31;
            int rt = (insn >> 16) & 31;
            int rs = (insn >> 21) & 31;


            return Dis_RDRTRS("sllv", rd, rt, rs);
        }
    };
    public static final Instruction SRA = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int sa = (insn >> 6) & 31;
            int rd = (insn >> 11) & 31;
            int rt = (insn >> 16) & 31;



            if (rd != 0) {
                processor.gpr[rd] = (processor.gpr[rt] >> sa);
            }

        }

        @Override
        public void compile(Processor processor, int insn) {
            int sa = (insn >> 6) & 31;
            int rd = (insn >> 11) & 31;
            int rt = (insn >> 16) & 31;



            if (rd == 0) {
                return;
            }
            if (processor.tracked_gpr[rt].fixed) {
                processor.fix_gpr(rd, (processor.gpr[rt] >> sa));
            } else {
                processor.load_gpr(rt, false);
                processor.alter_gpr(rd);
                processor.current_bb.emit(
                        processor.get_gpr(rd) +
                        " = (" +
                        processor.get_gpr(rt) +
                        " >> " +
                        sa +
                        ");");
            }

        }

        @Override
        public String disasm(int address, int insn) {
            int sa = (insn >> 6) & 31;
            int rd = (insn >> 11) & 31;
            int rt = (insn >> 16) & 31;


            return Dis_RDRTSA("sra", rd, rt, sa);
        }
    };
    public static final Instruction SRAV = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int rd = (insn >> 11) & 31;
            int rt = (insn >> 16) & 31;
            int rs = (insn >> 21) & 31;



            if (rd != 0) {
                processor.gpr[rd] = processor.gpr[rt] >> (processor.gpr[rs] & 31);
            }

        }

        @Override
        public void compile(Processor processor, int insn) {
            int rd = (insn >> 11) & 31;
            int rt = (insn >> 16) & 31;
            int rs = (insn >> 21) & 31;



            if (rd == 0) {
                return;
            }
            if (processor.tracked_gpr[rs].fixed && processor.tracked_gpr[rt].fixed) {
                processor.fix_gpr(rd, processor.gpr[rt] >> (processor.gpr[rs] & 31));
            } else {
                processor.load_gpr(rs, false);
                processor.load_gpr(rt, false);
                processor.alter_gpr(rd);
                processor.current_bb.emit(
                        processor.get_gpr(rd) +
                        " = (" +
                        processor.get_gpr(rt) +
                        " >> (" +
                        processor.get_gpr(rs) +
                        "&31));");
            }

        }

        @Override
        public String disasm(int address, int insn) {
            int rd = (insn >> 11) & 31;
            int rt = (insn >> 16) & 31;
            int rs = (insn >> 21) & 31;


            return Dis_RDRTRS("srav", rd, rt, rs);
        }
    };
    public static final Instruction SRL = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int sa = (insn >> 6) & 31;
            int rd = (insn >> 11) & 31;
            int rt = (insn >> 16) & 31;



            if (rd != 0) {
                processor.gpr[rd] = (processor.gpr[rt] >>> sa);
            }

        }

        @Override
        public void compile(Processor processor, int insn) {
            int sa = (insn >> 6) & 31;
            int rd = (insn >> 11) & 31;
            int rt = (insn >> 16) & 31;



            if (rd == 0) {
                return;
            }
            if (processor.tracked_gpr[rt].fixed) {
                processor.fix_gpr(rd, (processor.gpr[rt] >>> sa));
            } else {
                processor.load_gpr(rt, false);
                processor.alter_gpr(rd);
                processor.current_bb.emit(
                        processor.get_gpr(rd) +
                        " = (" +
                        processor.get_gpr(rt) +
                        " >>> " +
                        sa +
                        ");");
            }

        }

        @Override
        public String disasm(int address, int insn) {
            int sa = (insn >> 6) & 31;
            int rd = (insn >> 11) & 31;
            int rt = (insn >> 16) & 31;


            return Dis_RDRTSA("srl", rd, rt, sa);
        }
    };
    public static final Instruction SRLV = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int rd = (insn >> 11) & 31;
            int rt = (insn >> 16) & 31;
            int rs = (insn >> 21) & 31;



            if (rd != 0) {
                processor.gpr[rd] = processor.gpr[rt] >>> (processor.gpr[rs] & 31);
            }

        }

        @Override
        public void compile(Processor processor, int insn) {
            int rd = (insn >> 11) & 31;
            int rt = (insn >> 16) & 31;
            int rs = (insn >> 21) & 31;



            if (rd == 0) {
                return;
            }
            if (processor.tracked_gpr[rs].fixed && processor.tracked_gpr[rt].fixed) {
                processor.fix_gpr(rd, processor.gpr[rt] >>> (processor.gpr[rs] & 31));
            } else {
                processor.load_gpr(rs, false);
                processor.load_gpr(rt, false);
                processor.alter_gpr(rd);
                processor.current_bb.emit(
                        processor.get_gpr(rd) +
                        " = (" +
                        processor.get_gpr(rt) +
                        " >>> (" +
                        processor.get_gpr(rs) +
                        "&31));");
            }

        }

        @Override
        public String disasm(int address, int insn) {
            int rd = (insn >> 11) & 31;
            int rt = (insn >> 16) & 31;
            int rs = (insn >> 21) & 31;


            return Dis_RDRTRS("srlv", rd, rt, rs);
        }
    };
    public static final Instruction ROTR = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int sa = (insn >> 6) & 31;
            int rd = (insn >> 11) & 31;
            int rt = (insn >> 16) & 31;



            if (rd != 0) {
                processor.gpr[rd] = Integer.rotateRight(processor.gpr[rt], sa);
            }

        }

        @Override
        public void compile(Processor processor, int insn) {
            int sa = (insn >> 6) & 31;
            int rd = (insn >> 11) & 31;
            int rt = (insn >> 16) & 31;



            if (rd == 0) {
                return;
            }
            if (processor.tracked_gpr[rt].fixed) {
                processor.fix_gpr(rd, Integer.rotateRight(processor.gpr[rt], sa));
            } else {
                processor.load_gpr(rt, false);
                processor.alter_gpr(rd);
                processor.current_bb.emit(
                        processor.get_gpr(rd) +
                        " = Integer.rotateRight(" +
                        processor.get_gpr(rt) +
                        ", " +
                        sa +
                        ");");
            }

        }

        @Override
        public String disasm(int address, int insn) {
            int sa = (insn >> 6) & 31;
            int rd = (insn >> 11) & 31;
            int rt = (insn >> 16) & 31;


            return Dis_RDRTSA("rotr", rd, rt, sa);
        }
    };
    public static final Instruction ROTRV = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int rd = (insn >> 11) & 31;
            int rt = (insn >> 16) & 31;
            int rs = (insn >> 21) & 31;



            if (rd != 0) {
                processor.gpr[rd] = Integer.rotateRight(processor.gpr[rt], processor.gpr[rs]);
            }

        }

        @Override
        public void compile(Processor processor, int insn) {
            int rd = (insn >> 11) & 31;
            int rt = (insn >> 16) & 31;
            int rs = (insn >> 21) & 31;



            if (rd == 0) {
                return;
            }
            if (processor.tracked_gpr[rs].fixed && processor.tracked_gpr[rt].fixed) {
                processor.fix_gpr(rd, Integer.rotateRight(processor.gpr[rt], processor.gpr[rs]));
            } else {
                processor.load_gpr(rs, false);
                processor.load_gpr(rt, false);
                processor.alter_gpr(rd);
                processor.current_bb.emit(
                        processor.get_gpr(rd) +
                        " = Integer.rotateRight(" +
                        processor.get_gpr(rt) +
                        ", " +
                        processor.get_gpr(rs) +
                        ");");
            }

        }

        @Override
        public String disasm(int address, int insn) {
            int rd = (insn >> 11) & 31;
            int rt = (insn >> 16) & 31;
            int rs = (insn >> 21) & 31;


            return Dis_RDRTRS("rotrv", rd, rt, rs);
        }
    };
    public static final Instruction SLT = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int rd = (insn >> 11) & 31;
            int rt = (insn >> 16) & 31;
            int rs = (insn >> 21) & 31;



            if (rd != 0) {
                processor.gpr[rd] = Processor.signedCompare(processor.gpr[rs], processor.gpr[rt]);
            }

        }

        @Override
        public void compile(Processor processor, int insn) {
            int rd = (insn >> 11) & 31;
            int rt = (insn >> 16) & 31;
            int rs = (insn >> 21) & 31;



            if (rd == 0) {
                return;
            }
            if (processor.tracked_gpr[rs].fixed && processor.tracked_gpr[rt].fixed) {
                processor.fix_gpr(rd, Processor.signedCompare(processor.gpr[rs], processor.gpr[rt]));
            } else {
                processor.load_gpr(rs, false);
                processor.load_gpr(rt, false);
                processor.alter_gpr(rd);
                processor.current_bb.emit(
                        processor.get_gpr(rd) +
                        " = Processor.signedCompare(" +
                        processor.get_gpr(rs) +
                        ", " +
                        processor.get_gpr(rt) +
                        ");");
            }

        }

        @Override
        public String disasm(int address, int insn) {
            int rd = (insn >> 11) & 31;
            int rt = (insn >> 16) & 31;
            int rs = (insn >> 21) & 31;


            return Dis_RDRSRT("slt", rd, rs, rt);
        }
    };
    public static final Instruction SLTI = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int imm16 = (insn >> 0) & 65535;
            int rt = (insn >> 16) & 31;
            int rs = (insn >> 21) & 31;



            if (rt != 0) {
                processor.gpr[rt] = Processor.signedCompare(processor.gpr[rs], Processor.signExtend(imm16));
            }

        }

        @Override
        public void compile(Processor processor, int insn) {
            int imm16 = (insn >> 0) & 65535;
            int rt = (insn >> 16) & 31;
            int rs = (insn >> 21) & 31;



            if (rt == 0) {
                return;
            }
            if (processor.tracked_gpr[rs].fixed) {
                processor.fix_gpr(rt, Processor.signedCompare(processor.gpr[rs], Processor.signExtend(imm16)));
            } else {
                processor.load_gpr(rs, false);
                processor.alter_gpr(rt);
                processor.current_bb.emit(
                        processor.get_gpr(rt) +
                        " = Processor.signedCompare(" +
                        processor.get_gpr(rs) +
                        ", " +
                        Processor.signExtend(imm16) +
                        ");");
            }

        }

        @Override
        public String disasm(int address, int insn) {
            int imm16 = (insn >> 0) & 65535;
            int rt = (insn >> 16) & 31;
            int rs = (insn >> 21) & 31;


            return Dis_RTRSIMM("slti", rt, rs, signExtend(imm16));
        }
    };
    public static final Instruction SLTU = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int rd = (insn >> 11) & 31;
            int rt = (insn >> 16) & 31;
            int rs = (insn >> 21) & 31;



            if (rd != 0) {
                processor.gpr[rd] = Processor.unsignedCompare(processor.gpr[rt], processor.gpr[rs]);
            }

        }

        @Override
        public void compile(Processor processor, int insn) {
            int rd = (insn >> 11) & 31;
            int rt = (insn >> 16) & 31;
            int rs = (insn >> 21) & 31;



            if (rd == 0) {
                return;
            }
            if (processor.tracked_gpr[rs].fixed && processor.tracked_gpr[rt].fixed) {
                processor.fix_gpr(rd, Processor.unsignedCompare(processor.gpr[rt], processor.gpr[rs]));
            } else {
                processor.load_gpr(rs, false);
                processor.load_gpr(rt, false);
                processor.alter_gpr(rd);
                processor.current_bb.emit(
                        processor.get_gpr(rd) +
                        " = Processor.unsignedCompare(" +
                        processor.get_gpr(rs) +
                        ", " +
                        processor.get_gpr(rt) +
                        ");");
            }

        }

        @Override
        public String disasm(int address, int insn) {
            int rd = (insn >> 11) & 31;
            int rt = (insn >> 16) & 31;
            int rs = (insn >> 21) & 31;


            return Dis_RDRSRT("sltu", rd, rs, rt);
        }
    };
    public static final Instruction SLTIU = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int imm16 = (insn >> 0) & 65535;
            int rt = (insn >> 16) & 31;
            int rs = (insn >> 21) & 31;



            if (rt != 0) {
                processor.gpr[rt] = Processor.unsignedCompare(processor.gpr[rs], Processor.signExtend(imm16));
            }

        }

        @Override
        public void compile(Processor processor, int insn) {
            int imm16 = (insn >> 0) & 65535;
            int rt = (insn >> 16) & 31;
            int rs = (insn >> 21) & 31;



            if (rt == 0) {
                return;
            }
            if (processor.tracked_gpr[rs].fixed) {
                processor.fix_gpr(rt, Processor.unsignedCompare(processor.gpr[rs], Processor.signExtend(imm16)));
            } else {
                processor.load_gpr(rs, false);
                processor.alter_gpr(rt);
                processor.current_bb.emit(
                        processor.get_gpr(rt) +
                        " = Processor.unsignedCompare(" +
                        processor.get_gpr(rs) +
                        ", " +
                        Processor.signExtend(imm16) +
                        ");");
            }

        }

        @Override
        public String disasm(int address, int insn) {
            int imm16 = (insn >> 0) & 65535;
            int rt = (insn >> 16) & 31;
            int rs = (insn >> 21) & 31;


            return Dis_RTRSIMM("sltiu", rt, rs, signExtend(imm16));
        }
    };
    public static final Instruction SUB = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int rd = (insn >> 11) & 31;
            int rt = (insn >> 16) & 31;
            int rs = (insn >> 21) & 31;



            if (rd != 0) {
                processor.gpr[rd] = processor.gpr[rs] - processor.gpr[rt];
            }

        }

        @Override
        public void compile(Processor processor, int insn) {
            int rd = (insn >> 11) & 31;
            int rt = (insn >> 16) & 31;
            int rs = (insn >> 21) & 31;



            if (rd == 0) {
                return;
            }
            if (processor.tracked_gpr[rs].fixed && processor.tracked_gpr[rt].fixed) {
                processor.fix_gpr(rd, processor.gpr[rs] - processor.gpr[rt]);
            } else {
                processor.load_gpr(rs, false);
                processor.load_gpr(rt, false);
                processor.alter_gpr(rd);
                processor.current_bb.emit(
                        processor.get_gpr(rd) +
                        " = (" +
                        processor.get_gpr(rs) +
                        " - " +
                        processor.get_gpr(rt) +
                        ");");
            }

        }

        @Override
        public String disasm(int address, int insn) {
            int rd = (insn >> 11) & 31;
            int rt = (insn >> 16) & 31;
            int rs = (insn >> 21) & 31;


            return Dis_RDRSRT("sub", rd, rs, rt);
        }
    };
    public static final Instruction SUBU = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int rd = (insn >> 11) & 31;
            int rt = (insn >> 16) & 31;
            int rs = (insn >> 21) & 31;



            if (rd != 0) {
                processor.gpr[rd] = processor.gpr[rs] - processor.gpr[rt];
            }

        }

        @Override
        public void compile(Processor processor, int insn) {
            int rd = (insn >> 11) & 31;
            int rt = (insn >> 16) & 31;
            int rs = (insn >> 21) & 31;



            if (rd == 0) {
                return;
            }
            if (processor.tracked_gpr[rs].fixed && processor.tracked_gpr[rt].fixed) {
                processor.fix_gpr(rd, processor.gpr[rs] - processor.gpr[rt]);
            } else {
                processor.load_gpr(rs, false);
                processor.load_gpr(rt, false);
                processor.alter_gpr(rd);
                processor.current_bb.emit(
                        processor.get_gpr(rd) +
                        " = (" +
                        processor.get_gpr(rs) +
                        " - " +
                        processor.get_gpr(rt) +
                        ");");
            }

        }

        @Override
        public String disasm(int address, int insn) {
            int rd = (insn >> 11) & 31;
            int rt = (insn >> 16) & 31;
            int rs = (insn >> 21) & 31;


            return Dis_RDRSRT("subu", rd, rs, rt);
        }
    };
    public static final Instruction LUI = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int imm16 = (insn >> 0) & 65535;
            int rt = (insn >> 16) & 31;



            if (rt != 0) {
                processor.gpr[rt] = (imm16 << 16);
            }

        }

        @Override
        public void compile(Processor processor, int insn) {
            int imm16 = (insn >> 0) & 65535;
            int rt = (insn >> 16) & 31;



            if (rt == 0) {
                return;
            }
            processor.fix_gpr(rt, (imm16 << 16));

        }

        @Override
        public String disasm(int address, int insn) {
            int imm16 = (insn >> 0) & 65535;
            int rt = (insn >> 16) & 31;


            return Dis_RTIMM("lui", rt, zeroExtend(imm16));
        }
    };
    public static final Instruction SEB = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int rd = (insn >> 11) & 31;
            int rt = (insn >> 16) & 31;



            if (rd != 0) {
                processor.gpr[rd] = Processor.signExtend8(processor.gpr[rt]);
            }

        }

        @Override
        public void compile(Processor processor, int insn) {
            int rd = (insn >> 11) & 31;
            int rt = (insn >> 16) & 31;



            if (rd == 0) {
                return;
            }
            if (processor.tracked_gpr[rt].fixed) {
                processor.fix_gpr(rd, Processor.signExtend8(processor.gpr[rt]));
            } else {
                processor.load_gpr(rt, false);
                processor.alter_gpr(rd);
                processor.current_bb.emit(
                        processor.get_gpr(rd) +
                        " = Processor.signExtend8(" +
                        processor.get_gpr(rt) +
                        ");");
            }

        }

        @Override
        public String disasm(int address, int insn) {
            int rd = (insn >> 11) & 31;
            int rt = (insn >> 16) & 31;


            return Dis_RDRT("seb", rd, rt);
        }
    };
    public static final Instruction SEH = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int rd = (insn >> 11) & 31;
            int rt = (insn >> 16) & 31;



            if (rd != 0) {
                processor.gpr[rd] = Processor.signExtend(processor.gpr[rt]);
            }

        }

        @Override
        public void compile(Processor processor, int insn) {
            int rd = (insn >> 11) & 31;
            int rt = (insn >> 16) & 31;



            if (rd == 0) {
                return;
            }
            if (processor.tracked_gpr[rt].fixed) {
                processor.fix_gpr(rd, Processor.signExtend(processor.gpr[rt]));
            } else {
                processor.load_gpr(rt, false);
                processor.alter_gpr(rd);
                processor.current_bb.emit(
                        processor.get_gpr(rd) +
                        " = Processor.signExtend(" +
                        processor.get_gpr(rt) +
                        ");");
            }

        }

        @Override
        public String disasm(int address, int insn) {
            int rd = (insn >> 11) & 31;
            int rt = (insn >> 16) & 31;


            return Dis_RDRT("seh", rd, rt);
        }
    };
    public static final Instruction BITREV = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int rd = (insn >> 11) & 31;
            int rt = (insn >> 16) & 31;



            if (rd != 0) {
                processor.gpr[rd] = Integer.reverse(processor.gpr[rt]);
            }

        }

        @Override
        public void compile(Processor processor, int insn) {
            int rd = (insn >> 11) & 31;
            int rt = (insn >> 16) & 31;



            if (rd == 0) {
                return;
            }
            if (processor.tracked_gpr[rt].fixed) {
                processor.fix_gpr(rd, Integer.reverse(processor.gpr[rt]));
            } else {
                processor.load_gpr(rt, false);
                processor.alter_gpr(rd);
                processor.current_bb.emit(
                        processor.get_gpr(rd) +
                        " = Integer.reverse(" +
                        processor.get_gpr(rt) +
                        ");");
            }

        }

        @Override
        public String disasm(int address, int insn) {
            int rd = (insn >> 11) & 31;
            int rt = (insn >> 16) & 31;


            return Dis_RDRT("bitrev", rd, rt);
        }
    };
    public static final Instruction WSBH = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int rd = (insn >> 11) & 31;
            int rt = (insn >> 16) & 31;



            if (rd != 0) {
                processor.gpr[rd] = Integer.rotateRight(Integer.reverseBytes(processor.gpr[rt]), 16);
            }

        }

        @Override
        public void compile(Processor processor, int insn) {
            int rd = (insn >> 11) & 31;
            int rt = (insn >> 16) & 31;



            if (rd == 0) {
                return;
            }
            if (processor.tracked_gpr[rt].fixed) {
                processor.fix_gpr(rd, Integer.rotateRight(Integer.reverseBytes(processor.gpr[rt]), 16));
            } else {
                processor.load_gpr(rt, false);
                processor.alter_gpr(rd);
                processor.current_bb.emit(
                        processor.get_gpr(rd) +
                        " = Integer.rotateRight(Integer.reverseBytes(" +
                        processor.get_gpr(rt) +
                        "), 16);");
            }

        }

        @Override
        public String disasm(int address, int insn) {
            int rd = (insn >> 11) & 31;
            int rt = (insn >> 16) & 31;


            return Dis_RDRT("wsbh", rd, rt);
        }
    };
    public static final Instruction WSBW = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int rd = (insn >> 11) & 31;
            int rt = (insn >> 16) & 31;



            if (rd != 0) {
                processor.gpr[rd] = Integer.reverseBytes(processor.gpr[rt]);
            }

        }

        @Override
        public void compile(Processor processor, int insn) {
            int rd = (insn >> 11) & 31;
            int rt = (insn >> 16) & 31;



            if (rd == 0) {
                return;
            }
            if (processor.tracked_gpr[rt].fixed) {
                processor.fix_gpr(rd, Integer.reverseBytes(processor.gpr[rt]));
            } else {
                processor.load_gpr(rt, false);
                processor.alter_gpr(rd);
                processor.current_bb.emit(
                        processor.get_gpr(rd) +
                        " = Integer.reverseBytes(" +
                        processor.get_gpr(rt) +
                        ");");
            }

        }

        @Override
        public String disasm(int address, int insn) {
            int rd = (insn >> 11) & 31;
            int rt = (insn >> 16) & 31;


            return Dis_RDRT("wsbw", rd, rt);
        }
    };
    public static final Instruction MOVZ = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int rd = (insn >> 11) & 31;
            int rt = (insn >> 16) & 31;
            int rs = (insn >> 21) & 31;



            if ((rd != 0) && (processor.gpr[rt] == 0)) {
                processor.gpr[rd] = processor.gpr[rs];
            }

        }

        @Override
        public void compile(Processor processor, int insn) {
            int rd = (insn >> 11) & 31;
            int rt = (insn >> 16) & 31;
            int rs = (insn >> 21) & 31;



            if (rd != 0) {
                if (processor.tracked_gpr[rs].fixed && processor.tracked_gpr[rt].fixed) {
                    if (processor.gpr[rt] == 0) {
                        processor.fix_gpr(rd, processor.gpr[rs]);
                    }
                } else {
                    processor.load_gpr(rs, false);
                    processor.load_gpr(rt, false);
                    processor.alter_gpr(rd);
                    processor.current_bb.emit(
                            "if (" +
                            processor.get_gpr(rt) +
                            " == 0) {\n" +
                            processor.get_gpr(rd) +
                            " = " +
                            processor.get_gpr(rs) +
                            ";}\n");
                }
            }

        }

        @Override
        public String disasm(int address, int insn) {
            int rd = (insn >> 11) & 31;
            int rt = (insn >> 16) & 31;
            int rs = (insn >> 21) & 31;


            return Dis_RDRSRT("movz", rd, rs, rt);
        }
    };
    public static final Instruction MOVN = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int rd = (insn >> 11) & 31;
            int rt = (insn >> 16) & 31;
            int rs = (insn >> 21) & 31;



            if ((rd != 0) && (processor.gpr[rt] != 0)) {
                processor.gpr[rd] = processor.gpr[rs];
            }

        }

        @Override
        public void compile(Processor processor, int insn) {
            int rd = (insn >> 11) & 31;
            int rt = (insn >> 16) & 31;
            int rs = (insn >> 21) & 31;



            if (rd != 0) {
                if (processor.tracked_gpr[rs].fixed && processor.tracked_gpr[rt].fixed) {
                    if (processor.gpr[rt] != 0) {
                        processor.fix_gpr(rd, processor.gpr[rs]);
                    }
                } else {
                    processor.load_gpr(rs, false);
                    processor.load_gpr(rt, false);
                    processor.alter_gpr(rd);
                    processor.current_bb.emit(
                            "if (" +
                            processor.get_gpr(rt) +
                            " != 0) {\n" +
                            processor.get_gpr(rd) +
                            " = " +
                            processor.get_gpr(rs) +
                            ";}\n");
                }
            }

        }

        @Override
        public String disasm(int address, int insn) {
            int rd = (insn >> 11) & 31;
            int rt = (insn >> 16) & 31;
            int rs = (insn >> 21) & 31;


            return Dis_RDRSRT("movn", rd, rs, rt);
        }
    };
    public static final Instruction MAX = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int rd = (insn >> 11) & 31;
            int rt = (insn >> 16) & 31;
            int rs = (insn >> 21) & 31;



            if (rd != 0) {
                processor.gpr[rd] = Processor.max(processor.gpr[rs], processor.gpr[rt]);
            }

        }

        @Override
        public void compile(Processor processor, int insn) {
            int rd = (insn >> 11) & 31;
            int rt = (insn >> 16) & 31;
            int rs = (insn >> 21) & 31;



            if (rd != 0) {
                if (processor.tracked_gpr[rs].fixed && processor.tracked_gpr[rt].fixed) {
                    processor.fix_gpr(rd, Processor.max(processor.gpr[rs], processor.gpr[rt]));
                } else {
                    processor.load_gpr(rs, false);
                    processor.load_gpr(rt, false);
                    processor.alter_gpr(rd);
                    processor.current_bb.emit(
                            processor.get_gpr(rd) +
                            " = Processor.max(" +
                            processor.get_gpr(rs) +
                            ", " +
                            processor.get_gpr(rt) +
                            "));");
                }
            }

        }

        @Override
        public String disasm(int address, int insn) {
            int rd = (insn >> 11) & 31;
            int rt = (insn >> 16) & 31;
            int rs = (insn >> 21) & 31;


            return Dis_RDRSRT("max", rd, rs, rt);
        }
    };
    public static final Instruction MIN = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int rd = (insn >> 11) & 31;
            int rt = (insn >> 16) & 31;
            int rs = (insn >> 21) & 31;



            if (rd != 0) {
                processor.gpr[rd] = Processor.min(processor.gpr[rs], processor.gpr[rt]);
            }

        }

        @Override
        public void compile(Processor processor, int insn) {
            int rd = (insn >> 11) & 31;
            int rt = (insn >> 16) & 31;
            int rs = (insn >> 21) & 31;



            if (rd != 0) {
                if (processor.tracked_gpr[rs].fixed && processor.tracked_gpr[rt].fixed) {
                    processor.fix_gpr(rd, Processor.min(processor.gpr[rs], processor.gpr[rt]));
                } else {
                    processor.load_gpr(rs, false);
                    processor.load_gpr(rt, false);
                    processor.alter_gpr(rd);
                    processor.current_bb.emit(
                            processor.get_gpr(rd) +
                            " = Processor.min(" +
                            processor.get_gpr(rs) +
                            ", " +
                            processor.get_gpr(rt) +
                            "));");
                }
            }

        }

        @Override
        public String disasm(int address, int insn) {
            int rd = (insn >> 11) & 31;
            int rt = (insn >> 16) & 31;
            int rs = (insn >> 21) & 31;


            return Dis_RDRSRT("min", rd, rs, rt);
        }
    };
    public static final Instruction CLZ = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int rd = (insn >> 11) & 31;
            int rs = (insn >> 21) & 31;



            if (rd != 0) {
                processor.gpr[rd] = Integer.numberOfLeadingZeros(processor.gpr[rs]);
            }

        }

        @Override
        public void compile(Processor processor, int insn) {
            int rd = (insn >> 11) & 31;
            int rs = (insn >> 21) & 31;



            if (rd != 0) {
                if (processor.tracked_gpr[rs].fixed) {
                    processor.fix_gpr(rd, Integer.numberOfLeadingZeros(processor.gpr[rs]));
                } else {
                    processor.load_gpr(rs, false);
                    processor.alter_gpr(rd);
                    processor.current_bb.emit(
                            processor.get_gpr(rd) +
                            " = Integer.numberOfLeadingZeros(" +
                            processor.get_gpr(rs) +
                            ");");
                }
            }

        }

        @Override
        public String disasm(int address, int insn) {
            int rd = (insn >> 11) & 31;
            int rs = (insn >> 21) & 31;


            return Dis_RDRS("clz", rd, rs);
        }
    };
    public static final Instruction CLO = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int rd = (insn >> 11) & 31;
            int rs = (insn >> 21) & 31;



            if (rd != 0) {
                processor.gpr[rd] = Integer.numberOfLeadingZeros(~processor.gpr[rs]);
            }

        }

        @Override
        public void compile(Processor processor, int insn) {
            int rd = (insn >> 11) & 31;
            int rs = (insn >> 21) & 31;



            if (rd != 0) {
                if (processor.tracked_gpr[rs].fixed) {
                    processor.fix_gpr(rd, Integer.numberOfLeadingZeros(~processor.gpr[rs]));
                } else {
                    processor.load_gpr(rs, false);
                    processor.alter_gpr(rd);
                    processor.current_bb.emit(
                            processor.get_gpr(rd) +
                            " = Integer.numberOfLeadingZeros(~" +
                            processor.get_gpr(rs) +
                            ");");
                }
            }

        }

        @Override
        public String disasm(int address, int insn) {
            int rd = (insn >> 11) & 31;
            int rs = (insn >> 21) & 31;


            return Dis_RDRS("clo", rd, rs);
        }
    };
    public static final Instruction EXT = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int lsb = (insn >> 6) & 31;
            int msb = (insn >> 11) & 31;
            int rt = (insn >> 16) & 31;
            int rs = (insn >> 21) & 31;



            if (rt != 0) {
                processor.gpr[rt] = Processor.extractBits(processor.gpr[rs], lsb, msb + 1);
            }

        }

        @Override
        public void compile(Processor processor, int insn) {
            int lsb = (insn >> 6) & 31;
            int msb = (insn >> 11) & 31;
            int rt = (insn >> 16) & 31;
            int rs = (insn >> 21) & 31;



            if (rt != 0) {
                if (processor.tracked_gpr[rs].fixed) {
                    processor.fix_gpr(rt, Processor.extractBits(processor.gpr[rs], lsb, msb + 1));
                } else {
                    processor.load_gpr(rs, false);
                    processor.alter_gpr(rt);
                    processor.current_bb.emit(
                            processor.get_gpr(rt) +
                            " = Processor.extractBits(" +
                            processor.get_gpr(rs) +
                            ", " +
                            lsb +
                            ", " +
                            (msb + 1) +
                            ");");
                }
            }

        }

        @Override
        public String disasm(int address, int insn) {
            int lsb = (insn >> 6) & 31;
            int msb = (insn >> 11) & 31;
            int rt = (insn >> 16) & 31;
            int rs = (insn >> 21) & 31;


            return Dis_Ext(rt, rs, lsb, msb);
        }
    };
    public static final Instruction INS = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int lsb = (insn >> 6) & 31;
            int msb = (insn >> 11) & 31;
            int rt = (insn >> 16) & 31;
            int rs = (insn >> 21) & 31;



            if (rt != 0) {
                processor.gpr[rt] = Processor.insertBits(processor.gpr[rt], processor.gpr[rs], lsb, msb);
            }

        }

        @Override
        public void compile(Processor processor, int insn) {
            int lsb = (insn >> 6) & 31;
            int msb = (insn >> 11) & 31;
            int rt = (insn >> 16) & 31;
            int rs = (insn >> 21) & 31;



            if (rt != 0) {
                if (processor.tracked_gpr[rs].fixed) {
                    processor.fix_gpr(rt, Processor.insertBits(processor.gpr[rt], processor.gpr[rs], lsb, msb));
                } else {
                    processor.load_gpr(rs, false);
                    processor.load_gpr(rt, false);
                    processor.alter_gpr(rt);
                    processor.current_bb.emit(
                            processor.get_gpr(rt) +
                            " = Processor.insertBits(" +
                            processor.get_gpr(rt) +
                            ", " +
                            processor.get_gpr(rs) +
                            ", " +
                            lsb +
                            ", " +
                            msb +
                            ");");
                }
            }

        }

        @Override
        public String disasm(int address, int insn) {
            int lsb = (insn >> 6) & 31;
            int msb = (insn >> 11) & 31;
            int rt = (insn >> 16) & 31;
            int rs = (insn >> 21) & 31;


            return Dis_Ins(rt, rs, lsb, msb);
        }
    };
    public static final Instruction MULT = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int rt = (insn >> 16) & 31;
            int rs = (insn >> 21) & 31;



            processor.hilo = ((long) processor.gpr[rs]) * ((long) processor.gpr[rt]);

        }

        @Override
        public void compile(Processor processor, int insn) {
            int rt = (insn >> 16) & 31;
            int rs = (insn >> 21) & 31;



            processor.load_gpr(rs, false);
            processor.load_gpr(rt, false);
            processor.alter_hilo();
            processor.current_bb.emit(
                    processor.get_hilo() +
                    " = ((long) " +
                    processor.get_gpr(rs) +
                    ") * ((long) " +
                    processor.get_gpr(rt) +
                    ");");

        }

        @Override
        public String disasm(int address, int insn) {
            int rt = (insn >> 16) & 31;
            int rs = (insn >> 21) & 31;


            return Dis_RSRT("mult", rs, rt);
        }
    };
    public static final Instruction MULTU = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int rt = (insn >> 16) & 31;
            int rs = (insn >> 21) & 31;



            processor.hilo = (((long) processor.gpr[rs]) & 0xffffffff) * (((long) processor.gpr[rt]) & 0xffffffff);

        }

        @Override
        public void compile(Processor processor, int insn) {
            int rt = (insn >> 16) & 31;
            int rs = (insn >> 21) & 31;



            processor.load_gpr(rs, false);
            processor.load_gpr(rt, false);
            processor.alter_hilo();
            processor.current_bb.emit(
                    processor.get_hilo() +
                    " = (((long) " +
                    processor.get_gpr(rs) +
                    ") & 0xffffffff) * (((long) " +
                    processor.get_gpr(rt) +
                    ") & 0xffffffff);");

        }

        @Override
        public String disasm(int address, int insn) {
            int rt = (insn >> 16) & 31;
            int rs = (insn >> 21) & 31;


            return Dis_RSRT("multu", rs, rt);
        }
    };
    public static final Instruction MADD = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int rt = (insn >> 16) & 31;
            int rs = (insn >> 21) & 31;



            processor.hilo += ((long) processor.gpr[rs]) * ((long) processor.gpr[rt]);

        }

        @Override
        public void compile(Processor processor, int insn) {
            int rt = (insn >> 16) & 31;
            int rs = (insn >> 21) & 31;



            processor.load_gpr(rs, false);
            processor.load_gpr(rt, false);
            processor.load_hilo(false);
            processor.alter_hilo();
            processor.current_bb.emit(
                    processor.get_hilo() +
                    " += ((long) " +
                    processor.get_gpr(rs) +
                    ") * ((long) " +
                    processor.get_gpr(rt) +
                    ");");

        }

        @Override
        public String disasm(int address, int insn) {
            int rt = (insn >> 16) & 31;
            int rs = (insn >> 21) & 31;


            return Dis_RSRT("madd", rs, rt);
        }
    };
    public static final Instruction MADDU = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int rt = (insn >> 16) & 31;
            int rs = (insn >> 21) & 31;



            processor.hilo += (((long) processor.gpr[rs]) & 0xffffffff) * (((long) processor.gpr[rt]) & 0xffffffff);

        }

        @Override
        public void compile(Processor processor, int insn) {
            int rt = (insn >> 16) & 31;
            int rs = (insn >> 21) & 31;



            processor.load_gpr(rs, false);
            processor.load_gpr(rt, false);
            processor.load_hilo(false);
            processor.alter_hilo();
            processor.current_bb.emit(
                    processor.get_hilo() +
                    " += (((long) " +
                    processor.get_gpr(rs) +
                    ") & 0xffffffff) * (((long) " +
                    processor.get_gpr(rt) +
                    ") & 0xffffffff);");

        }

        @Override
        public String disasm(int address, int insn) {
            int rt = (insn >> 16) & 31;
            int rs = (insn >> 21) & 31;


            return Dis_RSRT("maddu", rs, rt);
        }
    };
    public static final Instruction MSUB = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int rt = (insn >> 16) & 31;
            int rs = (insn >> 21) & 31;



            processor.hilo -= ((long) processor.gpr[rs]) * ((long) processor.gpr[rt]);

        }

        @Override
        public void compile(Processor processor, int insn) {
            int rt = (insn >> 16) & 31;
            int rs = (insn >> 21) & 31;



            processor.load_gpr(rs, false);
            processor.load_gpr(rt, false);
            processor.load_hilo(false);
            processor.alter_hilo();
            processor.current_bb.emit(
                    processor.get_hilo() +
                    " += ((long) " +
                    processor.get_gpr(rs) +
                    ") * ((long) " +
                    processor.get_gpr(rt) +
                    ");");

        }

        @Override
        public String disasm(int address, int insn) {
            int rt = (insn >> 16) & 31;
            int rs = (insn >> 21) & 31;


            return Dis_RSRT("msub", rs, rt);
        }
    };
    public static final Instruction MSUBU = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int rt = (insn >> 16) & 31;
            int rs = (insn >> 21) & 31;



            processor.hilo -= (((long) processor.gpr[rs]) & 0xffffffff) * (((long) processor.gpr[rt]) & 0xffffffff);

        }

        @Override
        public void compile(Processor processor, int insn) {
            int rt = (insn >> 16) & 31;
            int rs = (insn >> 21) & 31;



            processor.load_gpr(rs, false);
            processor.load_gpr(rt, false);
            processor.load_hilo(false);
            processor.alter_hilo();
            processor.current_bb.emit(
                    processor.get_hilo() +
                    " -= (((long) " +
                    processor.get_gpr(rs) +
                    ") & 0xffffffff) * (((long) " +
                    processor.get_gpr(rt) +
                    ") & 0xffffffff);");

        }

        @Override
        public String disasm(int address, int insn) {
            int rt = (insn >> 16) & 31;
            int rs = (insn >> 21) & 31;


            return Dis_RSRT("msubu", rs, rt);
        }
    };
    public static final Instruction DIV = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int rt = (insn >> 16) & 31;
            int rs = (insn >> 21) & 31;



            processor.hilo = Processor.signedDivMod(processor.gpr[rs], processor.gpr[rt]);

        }

        @Override
        public void compile(Processor processor, int insn) {
            int rt = (insn >> 16) & 31;
            int rs = (insn >> 21) & 31;



            processor.load_gpr(rs, false);
            processor.load_gpr(rt, false);
            processor.alter_hilo();
            processor.current_bb.emit(
                    processor.get_hilo() +
                    " = Processor.signedDivMod(" +
                    processor.get_gpr(rs) +
                    ", " +
                    processor.get_gpr(rt) +
                    ");");

        }

        @Override
        public String disasm(int address, int insn) {
            int rt = (insn >> 16) & 31;
            int rs = (insn >> 21) & 31;


            return Dis_RSRT("div", rs, rt);
        }
    };
    public static final Instruction DIVU = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int rt = (insn >> 16) & 31;
            int rs = (insn >> 21) & 31;



            processor.hilo = Processor.unsignedDivMod((((long) processor.gpr[rs]) & 0xffffffff), (((long) processor.gpr[rt]) & 0xffffffff));

        }

        @Override
        public void compile(Processor processor, int insn) {
            int rt = (insn >> 16) & 31;
            int rs = (insn >> 21) & 31;



            processor.load_gpr(rs, false);
            processor.load_gpr(rt, false);
            processor.alter_hilo();
            processor.current_bb.emit(
                    processor.get_hilo() +
                    " = Processor.unsignedDivMod(((long) " +
                    processor.get_gpr(rs) +
                    ") & 0xffffffff, ((long) " +
                    processor.get_gpr(rt) +
                    ") & 0xffffffff);");

        }

        @Override
        public String disasm(int address, int insn) {
            int rt = (insn >> 16) & 31;
            int rs = (insn >> 21) & 31;


            return Dis_RSRT("divu", rs, rt);
        }
    };
    public static final Instruction MFHI = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int rd = (insn >> 11) & 31;



            if (rd != 0) {
                processor.gpr[rd] = processor.hi();
            }

        }

        @Override
        public void compile(Processor processor, int insn) {
            int rd = (insn >> 11) & 31;



            if (rd != 0) {
                processor.load_hilo(false);
                processor.alter_gpr(rd);
                processor.current_bb.emit(
                        processor.get_gpr(rd) +
                        " = (int) (" +
                        processor.get_hilo() +
                        " >>> 32);");
            }

        }

        @Override
        public String disasm(int address, int insn) {
            int rd = (insn >> 11) & 31;


            return Dis_RD("mfhi", rd);
        }
    };
    public static final Instruction MFLO = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int rd = (insn >> 11) & 31;



            if (rd != 0) {
                processor.gpr[rd] = processor.lo();
            }

        }

        @Override
        public void compile(Processor processor, int insn) {
            int rd = (insn >> 11) & 31;



            if (rd != 0) {
                processor.load_hilo(false);
                processor.alter_gpr(rd);
                processor.current_bb.emit(
                        processor.get_gpr(rd) +
                        " = (int) (" +
                        processor.get_hilo() +
                        " & 0xffffffff);");
            }

        }

        @Override
        public String disasm(int address, int insn) {
            int rd = (insn >> 11) & 31;


            return Dis_RD("mflo", rd);
        }
    };
    public static final Instruction MTHI = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int rs = (insn >> 21) & 31;



            processor.hilo = (processor.hilo & 0xffffffff) | (((long) processor.gpr[rs]) << 32);

        }

        @Override
        public void compile(Processor processor, int insn) {
            int rs = (insn >> 21) & 31;



            processor.load_gpr(rs, false);
            processor.alter_hilo();
            processor.current_bb.emit(
                    processor.get_hilo() +
                    " = (" +
                    processor.get_hilo() +
                    " & 0xffffffff) | (((long) " +
                    processor.get_gpr(rs) +
                    " << 32);");

        }

        @Override
        public String disasm(int address, int insn) {
            int rs = (insn >> 21) & 31;


            return Dis_RS("mthi", rs);
        }
    };
    public static final Instruction MTLO = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int rs = (insn >> 21) & 31;



            processor.hilo = ((processor.hilo >>> 32) << 32) | (((long) processor.gpr[rs]) & 0xffffffff);

        }

        @Override
        public void compile(Processor processor, int insn) {
            int rs = (insn >> 21) & 31;



            processor.load_gpr(rs, false);
            processor.alter_hilo();
            processor.current_bb.emit(
                    processor.get_hilo() +
                    " = ((" +
                    processor.get_hilo() +
                    " >>> 32) << 32) | (((long) " +
                    processor.get_gpr(rs) +
                    ") & 0xffffffff);");

        }

        @Override
        public String disasm(int address, int insn) {
            int rs = (insn >> 21) & 31;


            return Dis_RS("mtlo", rs);
        }
    };
    public static final Instruction BEQ = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int imm16 = (insn >> 0) & 65535;
            int rt = (insn >> 16) & 31;
            int rs = (insn >> 21) & 31;



            processor.npc = (processor.gpr[rs] == processor.gpr[rt]) ? Processor.branchTarget(processor.pc, Processor.signExtend(imm16)) : (processor.pc + 4);
            processor.stepDelayslot();

        }

        @Override
        public void compile(Processor processor, int insn) {
            int imm16 = (insn >> 0) & 65535;
            int rt = (insn >> 16) & 31;
            int rs = (insn >> 21) & 31;



            boolean c = (processor.gpr[rs] == processor.gpr[rt]);
            int target = Processor.branchTarget(processor.pc, Processor.signExtend(imm16));
            boolean loop = (target == processor.current_bb.getEntry());
            if (processor.tracked_gpr[rs].fixed && processor.tracked_gpr[rt].fixed) {
                if (c) {
                    processor.current_bb.emit("processor.pc = 0x" + Integer.toHexString(target) + ";");
                } else {
                    processor.current_bb.emit("processor.pc += 4;");
                }
            } else {
                processor.load_gpr(rs, false);
                processor.load_gpr(rt, false);
                processor.current_bb.emit(
                        "processor.pc = (" +
                        processor.get_gpr(rs) +
                        " == " +
                        processor.get_gpr(rt) +
                        ") ? 0x" +
                        Integer.toHexString(target) +
                        " : (processor.pc + 4);");
            }
            processor.npc = c ? target : (processor.pc + 4);
            processor.stepDelayslot();
            processor.reset_register_tracking();
            processor.current_bb.emit(loop ? "continue;\n" : "break;\n");
            processor.current_bb.freeze();
            processor.current_bb = null;

        }

        @Override
        public String disasm(int address, int insn) {
            int imm16 = (insn >> 0) & 65535;
            int rt = (insn >> 16) & 31;
            int rs = (insn >> 21) & 31;


            return Dis_RSRTOFFSET("beq", rs, rt, imm16, address);
        }
    };
    public static final Instruction BEQL = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int imm16 = (insn >> 0) & 65535;
            int rt = (insn >> 16) & 31;
            int rs = (insn >> 21) & 31;



            boolean c = (processor.gpr[rs] == processor.gpr[rt]);
            processor.npc = c ? Processor.branchTarget(processor.pc, Processor.signExtend(imm16)) : (processor.pc + 4);
            if (c) {
                processor.stepDelayslot();
            }

        }

        @Override
        public void compile(Processor processor, int insn) {
            int imm16 = (insn >> 0) & 65535;
            int rt = (insn >> 16) & 31;
            int rs = (insn >> 21) & 31;



            boolean c = (processor.gpr[rs] == processor.gpr[rt]);
            int target = Processor.branchTarget(processor.pc, Processor.signExtend(imm16));
            boolean loop = (target == processor.current_bb.getEntry());
            if (processor.tracked_gpr[rs].fixed && processor.tracked_gpr[rt].fixed) {
                if (c) {
                    processor.current_bb.emit("processor.pc = 0x" + Integer.toHexString(target) + ";");
                } else {
                    processor.current_bb.emit("processor.pc += 4;");
                }
            } else {
                processor.load_gpr(rs, false);
                processor.load_gpr(rt, false);
                processor.current_bb.emit(
                        "processor.pc = (" +
                        processor.get_gpr(rs) +
                        " == " +
                        processor.get_gpr(rt) +
                        ") ? 0x" +
                        Integer.toHexString(target) +
                        " : (processor.pc + 4);");
            }
            processor.npc = c ? target : (processor.pc + 4);
            if (c) {
                processor.stepDelayslot();
            }
            processor.reset_register_tracking();
            processor.current_bb.emit(loop ? "continue;\n" : "break;\n");
            processor.current_bb.freeze();
            processor.current_bb = null;

        }

        @Override
        public String disasm(int address, int insn) {
            int imm16 = (insn >> 0) & 65535;
            int rt = (insn >> 16) & 31;
            int rs = (insn >> 21) & 31;


            return Dis_RSRTOFFSET("beql", rs, rt, imm16, address);
        }
    };
    public static final Instruction BGEZ = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int imm16 = (insn >> 0) & 65535;
            int rs = (insn >> 21) & 31;




        }

        @Override
        public void compile(Processor processor, int insn) {
            int imm16 = (insn >> 0) & 65535;
            int rs = (insn >> 21) & 31;




        }

        @Override
        public String disasm(int address, int insn) {
            int imm16 = (insn >> 0) & 65535;
            int rs = (insn >> 21) & 31;


            return Dis_RSOFFSET("bgez", rs, imm16, address);
        }
    };
    public static final Instruction BGEZAL = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int imm16 = (insn >> 0) & 65535;
            int rs = (insn >> 21) & 31;




        }

        @Override
        public void compile(Processor processor, int insn) {
            int imm16 = (insn >> 0) & 65535;
            int rs = (insn >> 21) & 31;




        }

        @Override
        public String disasm(int address, int insn) {
            int imm16 = (insn >> 0) & 65535;
            int rs = (insn >> 21) & 31;


            return Dis_RSOFFSET("bgezal", rs, imm16, address);
        }
    };
    public static final Instruction BGEZALL = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int imm16 = (insn >> 0) & 65535;
            int rs = (insn >> 21) & 31;




        }

        @Override
        public void compile(Processor processor, int insn) {
            int imm16 = (insn >> 0) & 65535;
            int rs = (insn >> 21) & 31;




        }

        @Override
        public String disasm(int address, int insn) {
            int imm16 = (insn >> 0) & 65535;
            int rs = (insn >> 21) & 31;


            return Dis_RSOFFSET("bgezall", rs, imm16, address);
        }
    };
    public static final Instruction BGEZL = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int imm16 = (insn >> 0) & 65535;
            int rs = (insn >> 21) & 31;




        }

        @Override
        public void compile(Processor processor, int insn) {
            int imm16 = (insn >> 0) & 65535;
            int rs = (insn >> 21) & 31;




        }

        @Override
        public String disasm(int address, int insn) {
            int imm16 = (insn >> 0) & 65535;
            int rs = (insn >> 21) & 31;


            return Dis_RSOFFSET("bgezl", rs, imm16, address);
        }
    };
    public static final Instruction BGTZ = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int imm16 = (insn >> 0) & 65535;
            int rs = (insn >> 21) & 31;




        }

        @Override
        public void compile(Processor processor, int insn) {
            int imm16 = (insn >> 0) & 65535;
            int rs = (insn >> 21) & 31;




        }

        @Override
        public String disasm(int address, int insn) {
            int imm16 = (insn >> 0) & 65535;
            int rs = (insn >> 21) & 31;


            return Dis_RSOFFSET("bgtz", rs, imm16, address);
        }
    };
    public static final Instruction BGTZL = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int imm16 = (insn >> 0) & 65535;
            int rs = (insn >> 21) & 31;




        }

        @Override
        public void compile(Processor processor, int insn) {
            int imm16 = (insn >> 0) & 65535;
            int rs = (insn >> 21) & 31;




        }

        @Override
        public String disasm(int address, int insn) {
            int imm16 = (insn >> 0) & 65535;
            int rs = (insn >> 21) & 31;


            return Dis_RSOFFSET("bgtzl", rs, imm16, address);
        }
    };
    public static final Instruction BLEZ = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int imm16 = (insn >> 0) & 65535;
            int rs = (insn >> 21) & 31;




        }

        @Override
        public void compile(Processor processor, int insn) {
            int imm16 = (insn >> 0) & 65535;
            int rs = (insn >> 21) & 31;




        }

        @Override
        public String disasm(int address, int insn) {
            int imm16 = (insn >> 0) & 65535;
            int rs = (insn >> 21) & 31;


            return Dis_RSOFFSET("blez", rs, imm16, address);
        }
    };
    public static final Instruction BLEZL = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int imm16 = (insn >> 0) & 65535;
            int rs = (insn >> 21) & 31;




        }

        @Override
        public void compile(Processor processor, int insn) {
            int imm16 = (insn >> 0) & 65535;
            int rs = (insn >> 21) & 31;




        }

        @Override
        public String disasm(int address, int insn) {
            int imm16 = (insn >> 0) & 65535;
            int rs = (insn >> 21) & 31;


            return Dis_RSOFFSET("blezl", rs, imm16, address);
        }
    };
    public static final Instruction BLTZ = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int imm16 = (insn >> 0) & 65535;
            int rs = (insn >> 21) & 31;




        }

        @Override
        public void compile(Processor processor, int insn) {
            int imm16 = (insn >> 0) & 65535;
            int rs = (insn >> 21) & 31;




        }

        @Override
        public String disasm(int address, int insn) {
            int imm16 = (insn >> 0) & 65535;
            int rs = (insn >> 21) & 31;


            return Dis_RSOFFSET("bltz", rs, imm16, address);
        }
    };
    public static final Instruction BLTZAL = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int imm16 = (insn >> 0) & 65535;
            int rs = (insn >> 21) & 31;




        }

        @Override
        public void compile(Processor processor, int insn) {
            int imm16 = (insn >> 0) & 65535;
            int rs = (insn >> 21) & 31;




        }

        @Override
        public String disasm(int address, int insn) {
            int imm16 = (insn >> 0) & 65535;
            int rs = (insn >> 21) & 31;


            return Dis_RSOFFSET("bltzal", rs, imm16, address);
        }
    };
    public static final Instruction BLTZALL = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int imm16 = (insn >> 0) & 65535;
            int rs = (insn >> 21) & 31;




        }

        @Override
        public void compile(Processor processor, int insn) {
            int imm16 = (insn >> 0) & 65535;
            int rs = (insn >> 21) & 31;




        }

        @Override
        public String disasm(int address, int insn) {
            int imm16 = (insn >> 0) & 65535;
            int rs = (insn >> 21) & 31;


            return Dis_RSOFFSET("bltzall", rs, imm16, address);
        }
    };
    public static final Instruction BLTZL = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int imm16 = (insn >> 0) & 65535;
            int rs = (insn >> 21) & 31;




        }

        @Override
        public void compile(Processor processor, int insn) {
            int imm16 = (insn >> 0) & 65535;
            int rs = (insn >> 21) & 31;




        }

        @Override
        public String disasm(int address, int insn) {
            int imm16 = (insn >> 0) & 65535;
            int rs = (insn >> 21) & 31;


            return Dis_RSOFFSET("bltzl", rs, imm16, address);
        }
    };
    public static final Instruction BNE = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int imm16 = (insn >> 0) & 65535;
            int rt = (insn >> 16) & 31;
            int rs = (insn >> 21) & 31;



            processor.npc = (processor.gpr[rs] != processor.gpr[rt]) ? Processor.branchTarget(processor.pc, Processor.signExtend(imm16)) : (processor.pc + 4);
            processor.stepDelayslot();

        }

        @Override
        public void compile(Processor processor, int insn) {
            int imm16 = (insn >> 0) & 65535;
            int rt = (insn >> 16) & 31;
            int rs = (insn >> 21) & 31;



            boolean c = (processor.gpr[rs] != processor.gpr[rt]);
            int target = Processor.branchTarget(processor.pc, Processor.signExtend(imm16));
            boolean loop = (target == processor.current_bb.getEntry());
            if (processor.tracked_gpr[rs].fixed && processor.tracked_gpr[rt].fixed) {
                if (c) {
                    processor.current_bb.emit("processor.pc = 0x" + Integer.toHexString(target) + ";");
                } else {
                    processor.current_bb.emit("processor.pc += 4;");
                }
            } else {
                processor.load_gpr(rs, false);
                processor.load_gpr(rt, false);
                processor.current_bb.emit(
                        "processor.pc = (" +
                        processor.get_gpr(rs) +
                        " != " +
                        processor.get_gpr(rt) +
                        ") ? 0x" +
                        Integer.toHexString(target) +
                        " : (processor.pc + 4);");
            }
            processor.npc = c ? target : (processor.pc + 4);
            processor.stepDelayslot();
            processor.reset_register_tracking();
            processor.current_bb.emit(loop ? "continue;\n" : "break;\n");
            processor.current_bb.freeze();
            processor.current_bb = null;

        }

        @Override
        public String disasm(int address, int insn) {
            int imm16 = (insn >> 0) & 65535;
            int rt = (insn >> 16) & 31;
            int rs = (insn >> 21) & 31;


            return Dis_RSRTOFFSET("bne", rs, rt, imm16, address);
        }
    };
    public static final Instruction BNEL = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int imm16 = (insn >> 0) & 65535;
            int rt = (insn >> 16) & 31;
            int rs = (insn >> 21) & 31;




        }

        @Override
        public void compile(Processor processor, int insn) {
            int imm16 = (insn >> 0) & 65535;
            int rt = (insn >> 16) & 31;
            int rs = (insn >> 21) & 31;




        }

        @Override
        public String disasm(int address, int insn) {
            int imm16 = (insn >> 0) & 65535;
            int rt = (insn >> 16) & 31;
            int rs = (insn >> 21) & 31;


            return Dis_RSRTOFFSET("bnel", rs, rt, imm16, address);
        }
    };
    public static final Instruction J = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int imm26 = (insn >> 0) & 67108863;




        }

        @Override
        public void compile(Processor processor, int insn) {
            int imm26 = (insn >> 0) & 67108863;




        }

        @Override
        public String disasm(int address, int insn) {
            int imm26 = (insn >> 0) & 67108863;


            return Dis_JUMP("j", imm26, address);
        }
    };
    public static final Instruction JAL = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int imm26 = (insn >> 0) & 67108863;




        }

        @Override
        public void compile(Processor processor, int insn) {
            int imm26 = (insn >> 0) & 67108863;




        }

        @Override
        public String disasm(int address, int insn) {
            int imm26 = (insn >> 0) & 67108863;


            return Dis_JUMP("jal", imm26, address);
        }
    };
    public static final Instruction JALR = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int rd = (insn >> 11) & 31;
            int rs = (insn >> 21) & 31;



            if (rd != 0) {
                processor.gpr[rd] = processor.pc + 4;
            }
            processor.npc = processor.gpr[rs];
            processor.stepDelayslot();

        }

        @Override
        public void compile(Processor processor, int insn) {
            int rd = (insn >> 11) & 31;
            int rs = (insn >> 21) & 31;



            boolean loop = (processor.tracked_gpr[rs].fixed && (processor.gpr[rs] == processor.current_bb.getEntry()));
            if (rd != 0) {
                processor.fix_gpr(rd, processor.pc + 4);
            }
            processor.current_bb.emit(
                    "processor.pc = " + processor.get_gpr(rs) + ";");
            processor.npc = processor.gpr[rs];
            processor.stepDelayslot();
            processor.reset_register_tracking();
            processor.current_bb.emit("break;");
            processor.current_bb.freeze();
            processor.current_bb = null;

        }

        @Override
        public String disasm(int address, int insn) {
            int rd = (insn >> 11) & 31;
            int rs = (insn >> 21) & 31;


            return Dis_RDRS("jalr", rd, rs);
        }
    };
    public static final Instruction JR = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int rs = (insn >> 21) & 31;



            processor.npc = processor.gpr[rs];
            processor.stepDelayslot();

        }

        @Override
        public void compile(Processor processor, int insn) {
            int rs = (insn >> 21) & 31;



            boolean loop = (processor.tracked_gpr[rs].fixed && (processor.gpr[rs] == processor.current_bb.getEntry()));
            processor.current_bb.emit(
                    "processor.pc = " + processor.get_gpr(rs) + ";");
            processor.npc = processor.gpr[rs];
            processor.stepDelayslot();
            processor.reset_register_tracking();
            processor.current_bb.emit(loop ? "continue;" : "break;");
            processor.current_bb.freeze();
            processor.current_bb = null;

        }

        @Override
        public String disasm(int address, int insn) {
            int rs = (insn >> 21) & 31;


            return Dis_RS("jr", rs);
        }
    };
    public static final Instruction BC1F = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int imm16 = (insn >> 0) & 65535;



            processor.npc = !processor.fcr31_c ? Processor.branchTarget(processor.pc, Processor.signExtend(imm16)) : (processor.pc + 4);
            processor.stepDelayslot();

        }

        @Override
        public void compile(Processor processor, int insn) {
            int imm16 = (insn >> 0) & 65535;



            int target = Processor.branchTarget(processor.pc, Processor.signExtend(imm16));
            boolean loop = (target == processor.current_bb.getEntry());
            processor.current_bb.emit("processor.pc = (!processor.fcr31_c) ? 0x" + Integer.toHexString(target) + " : processor.pc + 4;");
            processor.npc = !processor.fcr31_c ? target : (processor.pc + 4);
            processor.stepDelayslot();
            processor.reset_register_tracking();
            processor.current_bb.emit(loop ? "continue;\n" : "break;\n");
            processor.current_bb.freeze();
            processor.current_bb = null;

        }

        @Override
        public String disasm(int address, int insn) {
            int imm16 = (insn >> 0) & 65535;


            return Dis_OFFSET("bc1f", imm16, address);
        }
    };
    public static final Instruction BC1T = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int imm16 = (insn >> 0) & 65535;



            processor.npc = processor.fcr31_c ? Processor.branchTarget(processor.pc, Processor.signExtend(imm16)) : (processor.pc + 4);
            processor.stepDelayslot();

        }

        @Override
        public void compile(Processor processor, int insn) {
            int imm16 = (insn >> 0) & 65535;



            int target = Processor.branchTarget(processor.pc, Processor.signExtend(imm16));
            boolean loop = (target == processor.current_bb.getEntry());
            processor.current_bb.emit("processor.pc = processor.fcr31_c ? 0x" + Integer.toHexString(target) + " : (processor.pc + 4);");
            processor.npc = processor.fcr31_c ? target : (processor.pc + 4);
            processor.stepDelayslot();
            processor.reset_register_tracking();
            processor.current_bb.emit(loop ? "continue;\n" : "break;\n");
            processor.current_bb.freeze();
            processor.current_bb = null;

        }

        @Override
        public String disasm(int address, int insn) {
            int imm16 = (insn >> 0) & 65535;


            return Dis_OFFSET("bc1t", imm16, address);
        }
    };
    public static final Instruction BC1FL = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int imm16 = (insn >> 0) & 65535;



            if (!processor.fcr31_c) {
                processor.npc = Processor.branchTarget(processor.pc, Processor.signExtend(imm16));
                processor.stepDelayslot();
            } else {
                processor.pc += 4;
            }

        }

        @Override
        public void compile(Processor processor, int insn) {
            int imm16 = (insn >> 0) & 65535;



            int target = Processor.branchTarget(processor.pc, Processor.signExtend(imm16));
            boolean loop = (target == processor.current_bb.getEntry());
            processor.current_bb.emit(
                    "if (!processor.fcr31_c) {\n" +
                    "processor.pc = 0x" + Integer.toHexString(target) + ";");
            processor.npc = !processor.fcr31_c ? target : (processor.pc + 4);
            processor.stepDelayslot();
            processor.current_bb.emit(
                    "} else {\n" +
                    "processor.pc += 4;" +
                    "}\n");
            processor.reset_register_tracking();
            processor.current_bb.emit(loop ? "continue;\n" : "break;\n");
            processor.current_bb.freeze();
            processor.current_bb = null;

        }

        @Override
        public String disasm(int address, int insn) {
            int imm16 = (insn >> 0) & 65535;


            return Dis_OFFSET("bc1fl", imm16, address);
        }
    };
    public static final Instruction BC1TL = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int imm16 = (insn >> 0) & 65535;



            if (processor.fcr31_c) {
                processor.npc = Processor.branchTarget(processor.pc, Processor.signExtend(imm16));
                processor.stepDelayslot();
            } else {
                processor.pc += 4;
            }

        }

        @Override
        public void compile(Processor processor, int insn) {
            int imm16 = (insn >> 0) & 65535;



            int target = Processor.branchTarget(processor.pc, Processor.signExtend(imm16));
            boolean loop = (target == processor.current_bb.getEntry());
            processor.current_bb.emit(
                    "if (processor.fcr31_c) {\n" +
                    "processor.pc = 0x" + Integer.toHexString(target) + ";");
            processor.npc = processor.fcr31_c ? target : (processor.pc + 4);
            processor.stepDelayslot();
            processor.current_bb.emit(
                    "} else {\n" +
                    "processor.pc += 4;" +
                    "}\n");
            processor.reset_register_tracking();
            processor.current_bb.emit(loop ? "continue;\n" : "break;\n");
            processor.current_bb.freeze();
            processor.current_bb = null;

        }

        @Override
        public String disasm(int address, int insn) {
            int imm16 = (insn >> 0) & 65535;


            return Dis_OFFSET("bc1tl", imm16, address);
        }
    };
    public static final Instruction LB = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int imm16 = (insn >> 0) & 65535;
            int rt = (insn >> 16) & 31;
            int rs = (insn >> 21) & 31;




        }

        @Override
        public void compile(Processor processor, int insn) {
            int imm16 = (insn >> 0) & 65535;
            int rt = (insn >> 16) & 31;
            int rs = (insn >> 21) & 31;




        }

        @Override
        public String disasm(int address, int insn) {
            int imm16 = (insn >> 0) & 65535;
            int rt = (insn >> 16) & 31;
            int rs = (insn >> 21) & 31;


            return Dis_RTIMMRS("lb", rt, rs, imm16);
        }
    };
    public static final Instruction LBU = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int imm16 = (insn >> 0) & 65535;
            int rt = (insn >> 16) & 31;
            int rs = (insn >> 21) & 31;




        }

        @Override
        public void compile(Processor processor, int insn) {
            int imm16 = (insn >> 0) & 65535;
            int rt = (insn >> 16) & 31;
            int rs = (insn >> 21) & 31;




        }

        @Override
        public String disasm(int address, int insn) {
            int imm16 = (insn >> 0) & 65535;
            int rt = (insn >> 16) & 31;
            int rs = (insn >> 21) & 31;


            return Dis_RTIMMRS("lbu", rt, rs, imm16);
        }
    };
    public static final Instruction LH = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int imm16 = (insn >> 0) & 65535;
            int rt = (insn >> 16) & 31;
            int rs = (insn >> 21) & 31;




        }

        @Override
        public void compile(Processor processor, int insn) {
            int imm16 = (insn >> 0) & 65535;
            int rt = (insn >> 16) & 31;
            int rs = (insn >> 21) & 31;




        }

        @Override
        public String disasm(int address, int insn) {
            int imm16 = (insn >> 0) & 65535;
            int rt = (insn >> 16) & 31;
            int rs = (insn >> 21) & 31;


            return Dis_RTIMMRS("lh", rt, rs, imm16);
        }
    };
    public static final Instruction LHU = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int imm16 = (insn >> 0) & 65535;
            int rt = (insn >> 16) & 31;
            int rs = (insn >> 21) & 31;




        }

        @Override
        public void compile(Processor processor, int insn) {
            int imm16 = (insn >> 0) & 65535;
            int rt = (insn >> 16) & 31;
            int rs = (insn >> 21) & 31;




        }

        @Override
        public String disasm(int address, int insn) {
            int imm16 = (insn >> 0) & 65535;
            int rt = (insn >> 16) & 31;
            int rs = (insn >> 21) & 31;


            return Dis_RTIMMRS("lhu", rt, rs, imm16);
        }
    };
    public static final Instruction LW = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int imm16 = (insn >> 0) & 65535;
            int rt = (insn >> 16) & 31;
            int rs = (insn >> 21) & 31;




        }

        @Override
        public void compile(Processor processor, int insn) {
            int imm16 = (insn >> 0) & 65535;
            int rt = (insn >> 16) & 31;
            int rs = (insn >> 21) & 31;




        }

        @Override
        public String disasm(int address, int insn) {
            int imm16 = (insn >> 0) & 65535;
            int rt = (insn >> 16) & 31;
            int rs = (insn >> 21) & 31;


            return Dis_RTIMMRS("lw", rt, rs, imm16);
        }
    };
    public static final Instruction LWL = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int imm16 = (insn >> 0) & 65535;
            int rt = (insn >> 16) & 31;
            int rs = (insn >> 21) & 31;




        }

        @Override
        public void compile(Processor processor, int insn) {
            int imm16 = (insn >> 0) & 65535;
            int rt = (insn >> 16) & 31;
            int rs = (insn >> 21) & 31;




        }

        @Override
        public String disasm(int address, int insn) {
            int imm16 = (insn >> 0) & 65535;
            int rt = (insn >> 16) & 31;
            int rs = (insn >> 21) & 31;


            return Dis_RTIMMRS("lwl", rt, rs, imm16);
        }
    };
    public static final Instruction LWR = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int imm16 = (insn >> 0) & 65535;
            int rt = (insn >> 16) & 31;
            int rs = (insn >> 21) & 31;




        }

        @Override
        public void compile(Processor processor, int insn) {
            int imm16 = (insn >> 0) & 65535;
            int rt = (insn >> 16) & 31;
            int rs = (insn >> 21) & 31;




        }

        @Override
        public String disasm(int address, int insn) {
            int imm16 = (insn >> 0) & 65535;
            int rt = (insn >> 16) & 31;
            int rs = (insn >> 21) & 31;


            return Dis_RTIMMRS("lwr", rt, rs, imm16);
        }
    };
    public static final Instruction SB = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int imm16 = (insn >> 0) & 65535;
            int rt = (insn >> 16) & 31;
            int rs = (insn >> 21) & 31;




        }

        @Override
        public void compile(Processor processor, int insn) {
            int imm16 = (insn >> 0) & 65535;
            int rt = (insn >> 16) & 31;
            int rs = (insn >> 21) & 31;




        }

        @Override
        public String disasm(int address, int insn) {
            int imm16 = (insn >> 0) & 65535;
            int rt = (insn >> 16) & 31;
            int rs = (insn >> 21) & 31;


            return Dis_RTIMMRS("sb", rt, rs, imm16);
        }
    };
    public static final Instruction SH = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int imm16 = (insn >> 0) & 65535;
            int rt = (insn >> 16) & 31;
            int rs = (insn >> 21) & 31;




        }

        @Override
        public void compile(Processor processor, int insn) {
            int imm16 = (insn >> 0) & 65535;
            int rt = (insn >> 16) & 31;
            int rs = (insn >> 21) & 31;




        }

        @Override
        public String disasm(int address, int insn) {
            int imm16 = (insn >> 0) & 65535;
            int rt = (insn >> 16) & 31;
            int rs = (insn >> 21) & 31;


            return Dis_RTIMMRS("sh", rt, rs, imm16);
        }
    };
    public static final Instruction SW = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int imm16 = (insn >> 0) & 65535;
            int rt = (insn >> 16) & 31;
            int rs = (insn >> 21) & 31;




        }

        @Override
        public void compile(Processor processor, int insn) {
            int imm16 = (insn >> 0) & 65535;
            int rt = (insn >> 16) & 31;
            int rs = (insn >> 21) & 31;




        }

        @Override
        public String disasm(int address, int insn) {
            int imm16 = (insn >> 0) & 65535;
            int rt = (insn >> 16) & 31;
            int rs = (insn >> 21) & 31;


            return Dis_RTIMMRS("sw", rt, rs, imm16);
        }
    };
    public static final Instruction SWL = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int imm16 = (insn >> 0) & 65535;
            int rt = (insn >> 16) & 31;
            int rs = (insn >> 21) & 31;




        }

        @Override
        public void compile(Processor processor, int insn) {
            int imm16 = (insn >> 0) & 65535;
            int rt = (insn >> 16) & 31;
            int rs = (insn >> 21) & 31;




        }

        @Override
        public String disasm(int address, int insn) {
            int imm16 = (insn >> 0) & 65535;
            int rt = (insn >> 16) & 31;
            int rs = (insn >> 21) & 31;


            return Dis_RTIMMRS("swl", rt, rs, imm16);
        }
    };
    public static final Instruction SWR = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int imm16 = (insn >> 0) & 65535;
            int rt = (insn >> 16) & 31;
            int rs = (insn >> 21) & 31;




        }

        @Override
        public void compile(Processor processor, int insn) {
            int imm16 = (insn >> 0) & 65535;
            int rt = (insn >> 16) & 31;
            int rs = (insn >> 21) & 31;




        }

        @Override
        public String disasm(int address, int insn) {
            int imm16 = (insn >> 0) & 65535;
            int rt = (insn >> 16) & 31;
            int rs = (insn >> 21) & 31;


            return Dis_RTIMMRS("swr", rt, rs, imm16);
        }
    };
    public static final Instruction LL = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int imm16 = (insn >> 0) & 65535;
            int rt = (insn >> 16) & 31;
            int rs = (insn >> 21) & 31;




        }

        @Override
        public void compile(Processor processor, int insn) {
            int imm16 = (insn >> 0) & 65535;
            int rt = (insn >> 16) & 31;
            int rs = (insn >> 21) & 31;




        }

        @Override
        public String disasm(int address, int insn) {
            int imm16 = (insn >> 0) & 65535;
            int rt = (insn >> 16) & 31;
            int rs = (insn >> 21) & 31;


            return Dis_RTIMMRS("ll", rt, rs, imm16);
        }
    };
    public static final Instruction SC = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int imm16 = (insn >> 0) & 65535;
            int rt = (insn >> 16) & 31;
            int rs = (insn >> 21) & 31;




        }

        @Override
        public void compile(Processor processor, int insn) {
            int imm16 = (insn >> 0) & 65535;
            int rt = (insn >> 16) & 31;
            int rs = (insn >> 21) & 31;




        }

        @Override
        public String disasm(int address, int insn) {
            int imm16 = (insn >> 0) & 65535;
            int rt = (insn >> 16) & 31;
            int rs = (insn >> 21) & 31;


            return Dis_RTIMMRS("sc", rt, rs, imm16);
        }
    };
    public static final Instruction LWC1 = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int imm16 = (insn >> 0) & 65535;
            int ft = (insn >> 16) & 31;
            int rs = (insn >> 21) & 31;




        }

        @Override
        public void compile(Processor processor, int insn) {
            int imm16 = (insn >> 0) & 65535;
            int ft = (insn >> 16) & 31;
            int rs = (insn >> 21) & 31;




        }

        @Override
        public String disasm(int address, int insn) {
            int imm16 = (insn >> 0) & 65535;
            int ft = (insn >> 16) & 31;
            int rs = (insn >> 21) & 31;


            return Dis_FTIMMRS("lwc1", ft, rs, imm16);
        }
    };
    public static final Instruction SWC1 = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int imm16 = (insn >> 0) & 65535;
            int ft = (insn >> 16) & 31;
            int rs = (insn >> 21) & 31;




        }

        @Override
        public void compile(Processor processor, int insn) {
            int imm16 = (insn >> 0) & 65535;
            int ft = (insn >> 16) & 31;
            int rs = (insn >> 21) & 31;




        }

        @Override
        public String disasm(int address, int insn) {
            int imm16 = (insn >> 0) & 65535;
            int ft = (insn >> 16) & 31;
            int rs = (insn >> 21) & 31;


            return Dis_FTIMMRS("swc1", ft, rs, imm16);
        }
    };
    public static final Instruction ADD_S = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int fd = (insn >> 6) & 31;
            int fs = (insn >> 11) & 31;
            int ft = (insn >> 16) & 31;



            processor.fpr[fd] = processor.fpr[fs] + processor.fpr[ft];

        }

        @Override
        public void compile(Processor processor, int insn) {
            int fd = (insn >> 6) & 31;
            int fs = (insn >> 11) & 31;
            int ft = (insn >> 16) & 31;



            if (processor.tracked_fpr[fs].fixed & processor.tracked_fpr[ft].fixed) {
                processor.fix_fpr(fd, processor.fpr[fs] + processor.fpr[ft]);
            } else {
                processor.load_fpr(fs, false);
                processor.load_fpr(ft, false);
                processor.alter_fpr(fd);
                processor.current_bb.emit(
                        processor.get_fpr(fd) +
                        " = (" +
                        processor.get_fpr(fs) +
                        " + " +
                        processor.get_fpr(ft) +
                        ");");
            }

        }

        @Override
        public String disasm(int address, int insn) {
            int fd = (insn >> 6) & 31;
            int fs = (insn >> 11) & 31;
            int ft = (insn >> 16) & 31;


            return Dis_FDFSFT("add.s", fd, fs, ft);
        }
    };
    public static final Instruction SUB_S = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int fd = (insn >> 6) & 31;
            int fs = (insn >> 11) & 31;
            int ft = (insn >> 16) & 31;



            processor.fpr[fd] = processor.fpr[fs] - processor.fpr[ft];

        }

        @Override
        public void compile(Processor processor, int insn) {
            int fd = (insn >> 6) & 31;
            int fs = (insn >> 11) & 31;
            int ft = (insn >> 16) & 31;



            if (processor.tracked_fpr[fs].fixed & processor.tracked_fpr[ft].fixed) {
                processor.fix_fpr(fd, processor.fpr[fs] - processor.fpr[ft]);
            } else {
                processor.load_fpr(fs, false);
                processor.load_fpr(ft, false);
                processor.alter_fpr(fd);
                processor.current_bb.emit(
                        processor.get_fpr(fd) +
                        " = (" +
                        processor.get_fpr(fs) +
                        " - " +
                        processor.get_fpr(ft) +
                        ");");
            }

        }

        @Override
        public String disasm(int address, int insn) {
            int fd = (insn >> 6) & 31;
            int fs = (insn >> 11) & 31;
            int ft = (insn >> 16) & 31;


            return Dis_FDFSFT("sub.s", fd, fs, ft);
        }
    };
    public static final Instruction MUL_S = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int fd = (insn >> 6) & 31;
            int fs = (insn >> 11) & 31;
            int ft = (insn >> 16) & 31;



            processor.fpr[fd] = processor.fpr[fs] * processor.fpr[ft];

        }

        @Override
        public void compile(Processor processor, int insn) {
            int fd = (insn >> 6) & 31;
            int fs = (insn >> 11) & 31;
            int ft = (insn >> 16) & 31;



            if (processor.tracked_fpr[fs].fixed & processor.tracked_fpr[ft].fixed) {
                processor.fix_fpr(fd, processor.fpr[fs] * processor.fpr[ft]);
            } else {
                processor.load_fpr(fs, false);
                processor.load_fpr(ft, false);
                processor.alter_fpr(fd);
                processor.current_bb.emit(
                        processor.get_fpr(fd) +
                        " = (" +
                        processor.get_fpr(fs) +
                        " * " +
                        processor.get_fpr(ft) +
                        ");");
            }

        }

        @Override
        public String disasm(int address, int insn) {
            int fd = (insn >> 6) & 31;
            int fs = (insn >> 11) & 31;
            int ft = (insn >> 16) & 31;


            return Dis_FDFSFT("mul.s", fd, fs, ft);
        }
    };
    public static final Instruction DIV_S = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int fd = (insn >> 6) & 31;
            int fs = (insn >> 11) & 31;
            int ft = (insn >> 16) & 31;



            processor.fpr[fd] = processor.fpr[fs] / processor.fpr[ft];

        }

        @Override
        public void compile(Processor processor, int insn) {
            int fd = (insn >> 6) & 31;
            int fs = (insn >> 11) & 31;
            int ft = (insn >> 16) & 31;



            if (processor.tracked_fpr[fs].fixed & processor.tracked_fpr[ft].fixed) {
                processor.fix_fpr(fd, processor.fpr[fs] / processor.fpr[ft]);
            } else {
                processor.load_fpr(fs, false);
                processor.load_fpr(ft, false);
                processor.alter_fpr(fd);
                processor.current_bb.emit(
                        processor.get_fpr(fd) +
                        " = (" +
                        processor.get_fpr(fs) +
                        " / " +
                        processor.get_fpr(ft) +
                        ");");
            }

        }

        @Override
        public String disasm(int address, int insn) {
            int fd = (insn >> 6) & 31;
            int fs = (insn >> 11) & 31;
            int ft = (insn >> 16) & 31;


            return Dis_FDFSFT("div.s", fd, fs, ft);
        }
    };
    public static final Instruction SQRT_S = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int fd = (insn >> 6) & 31;
            int fs = (insn >> 11) & 31;



            processor.fpr[fd] = (float) Math.sqrt(processor.fpr[fs]);

        }

        @Override
        public void compile(Processor processor, int insn) {
            int fd = (insn >> 6) & 31;
            int fs = (insn >> 11) & 31;



            if (processor.tracked_fpr[fs].fixed) {
                processor.fix_fpr(fd, (float) Math.sqrt(processor.fpr[fs]));
            } else {
                processor.load_fpr(fs, false);
                processor.alter_fpr(fd);
                processor.current_bb.emit(
                        processor.get_fpr(fd) +
                        " = (float) Math.sqrt(" +
                        processor.get_fpr(fs) +
                        ");");
            }

        }

        @Override
        public String disasm(int address, int insn) {
            int fd = (insn >> 6) & 31;
            int fs = (insn >> 11) & 31;


            return Dis_FDFS("sqrt.s", fd, fs);
        }
    };
    public static final Instruction ABS_S = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int fd = (insn >> 6) & 31;
            int fs = (insn >> 11) & 31;



            processor.fpr[fd] = Math.abs(processor.fpr[fs]);

        }

        @Override
        public void compile(Processor processor, int insn) {
            int fd = (insn >> 6) & 31;
            int fs = (insn >> 11) & 31;



            if (processor.tracked_fpr[fs].fixed) {
                processor.fix_fpr(fd, Math.abs(processor.fpr[fs]));
            } else {
                processor.load_fpr(fs, false);
                processor.alter_fpr(fd);
                processor.current_bb.emit(
                        processor.get_fpr(fd) +
                        " = Math.abs(" +
                        processor.get_fpr(fs) +
                        ");");
            }

        }

        @Override
        public String disasm(int address, int insn) {
            int fd = (insn >> 6) & 31;
            int fs = (insn >> 11) & 31;


            return Dis_FDFS("abs.s", fd, fs);
        }
    };
    public static final Instruction MOV_S = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int fd = (insn >> 6) & 31;
            int fs = (insn >> 11) & 31;



            processor.fpr[fd] = processor.fpr[fs];

        }

        @Override
        public void compile(Processor processor, int insn) {
            int fd = (insn >> 6) & 31;
            int fs = (insn >> 11) & 31;



            if (processor.tracked_fpr[fs].fixed) {
                processor.fix_fpr(fd, processor.fpr[fs]);
            } else {
                processor.load_fpr(fs, false);
                processor.alter_fpr(fd);
                processor.current_bb.emit(
                        processor.get_fpr(fd) +
                        " = (" +
                        processor.get_fpr(fs) +
                        ");");
            }

        }

        @Override
        public String disasm(int address, int insn) {
            int fd = (insn >> 6) & 31;
            int fs = (insn >> 11) & 31;


            return Dis_FDFS("mov.s", fd, fs);
        }
    };
    public static final Instruction NEG_S = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int fd = (insn >> 6) & 31;
            int fs = (insn >> 11) & 31;



            processor.fpr[fd] = 0.0f - processor.fpr[fs];

        }

        @Override
        public void compile(Processor processor, int insn) {
            int fd = (insn >> 6) & 31;
            int fs = (insn >> 11) & 31;



            if (processor.tracked_fpr[fs].fixed) {
                processor.fix_fpr(fd, 0.0f - processor.fpr[fs]);
            } else {
                processor.load_fpr(fs, false);
                processor.alter_fpr(fd);
                processor.current_bb.emit(
                        processor.get_fpr(fd) +
                        " = 0.0f - (" +
                        processor.get_fpr(fs) +
                        ");");
            }

        }

        @Override
        public String disasm(int address, int insn) {
            int fd = (insn >> 6) & 31;
            int fs = (insn >> 11) & 31;


            return Dis_FDFS("neg.s", fd, fs);
        }
    };
    public static final Instruction ROUND_W_S = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int fd = (insn >> 6) & 31;
            int fs = (insn >> 11) & 31;



            processor.fpr[fd] = Float.intBitsToFloat(Math.round(processor.fpr[fs]));

        }

        @Override
        public void compile(Processor processor, int insn) {
            int fd = (insn >> 6) & 31;
            int fs = (insn >> 11) & 31;



            if (processor.tracked_fpr[fs].fixed) {
                processor.fix_fpr(fd, Float.intBitsToFloat(Math.round(processor.fpr[fs])));
            } else {
                processor.load_fpr(fs, false);
                processor.alter_fpr(fd);
                processor.current_bb.emit(
                        processor.get_fpr(fd) +
                        " = Float.intBitsToFloat(Math.round(" +
                        processor.get_fpr(fs) +
                        "));");
            }

        }

        @Override
        public String disasm(int address, int insn) {
            int fd = (insn >> 6) & 31;
            int fs = (insn >> 11) & 31;


            return Dis_FDFS("round.w.s", fd, fs);
        }
    };
    public static final Instruction TRUNC_W_S = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int fd = (insn >> 6) & 31;
            int fs = (insn >> 11) & 31;



            processor.fpr[fd] = Float.intBitsToFloat((int) (processor.fpr[fs]));

        }

        @Override
        public void compile(Processor processor, int insn) {
            int fd = (insn >> 6) & 31;
            int fs = (insn >> 11) & 31;



            if (processor.tracked_fpr[fs].fixed) {
                processor.fix_fpr(fd, Float.intBitsToFloat((int) (processor.fpr[fs])));
            } else {
                processor.load_fpr(fs, false);
                processor.alter_fpr(fd);
                processor.current_bb.emit(
                        processor.get_fpr(fd) +
                        " = Float.intBitsToFloat((int) (" +
                        processor.get_fpr(fs) +
                        "));");
            }

        }

        @Override
        public String disasm(int address, int insn) {
            int fd = (insn >> 6) & 31;
            int fs = (insn >> 11) & 31;


            return Dis_FDFS("trunc.w.s", fd, fs);
        }
    };
    public static final Instruction CEIL_W_S = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int fd = (insn >> 6) & 31;
            int fs = (insn >> 11) & 31;



            processor.fpr[fd] = Float.intBitsToFloat((int) Math.ceil(processor.fpr[fs]));

        }

        @Override
        public void compile(Processor processor, int insn) {
            int fd = (insn >> 6) & 31;
            int fs = (insn >> 11) & 31;



            if (processor.tracked_fpr[fs].fixed) {
                processor.fix_fpr(fd, Float.intBitsToFloat((int) Math.ceil(processor.fpr[fs])));
            } else {
                processor.load_fpr(fs, false);
                processor.alter_fpr(fd);
                processor.current_bb.emit(
                        processor.get_fpr(fd) +
                        " = Float.intBitsToFloat((int) Math.ceil(" +
                        processor.get_fpr(fs) +
                        "));");
            }

        }

        @Override
        public String disasm(int address, int insn) {
            int fd = (insn >> 6) & 31;
            int fs = (insn >> 11) & 31;


            return Dis_FDFS("ceil.w.s", fd, fs);
        }
    };
    public static final Instruction FLOOR_W_S = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int fd = (insn >> 6) & 31;
            int fs = (insn >> 11) & 31;



            processor.fpr[fd] = Float.intBitsToFloat((int) Math.floor(processor.fpr[fs]));

        }

        @Override
        public void compile(Processor processor, int insn) {
            int fd = (insn >> 6) & 31;
            int fs = (insn >> 11) & 31;



            if (processor.tracked_fpr[fs].fixed) {
                processor.fix_fpr(fd, Float.intBitsToFloat((int) Math.floor(processor.fpr[fs])));
            } else {
                processor.load_fpr(fs, false);
                processor.alter_fpr(fd);
                processor.current_bb.emit(
                        processor.get_fpr(fd) +
                        " = Float.intBitsToFloat((int) Math.floor(" +
                        processor.get_fpr(fs) +
                        "));");
            }

        }

        @Override
        public String disasm(int address, int insn) {
            int fd = (insn >> 6) & 31;
            int fs = (insn >> 11) & 31;


            return Dis_FDFS("floor.w.s", fd, fs);
        }
    };
    public static final Instruction CVT_S_W = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int fd = (insn >> 6) & 31;
            int fs = (insn >> 11) & 31;



            processor.fpr[fd] = (float) Float.floatToRawIntBits(processor.fpr[fs]);

        }

        @Override
        public void compile(Processor processor, int insn) {
            int fd = (insn >> 6) & 31;
            int fs = (insn >> 11) & 31;



            if (processor.tracked_fpr[fs].fixed) {
                processor.fix_fpr(fd, (float) Float.floatToRawIntBits(processor.fpr[fs]));
            } else {
                processor.load_fpr(fs, false);
                processor.alter_fpr(fd);
                processor.current_bb.emit(
                        processor.get_fpr(fd) +
                        " = (float) Float.floatToRawIntBits(" +
                        processor.get_fpr(fs) +
                        ");");
            }

        }

        @Override
        public String disasm(int address, int insn) {
            int fd = (insn >> 6) & 31;
            int fs = (insn >> 11) & 31;


            return Dis_FDFS("cvt.s.w", fd, fs);
        }
    };
    public static final Instruction CVT_W_S = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int fd = (insn >> 6) & 31;
            int fs = (insn >> 11) & 31;



            switch (processor.fcr31_rm) {
                case 1:
                    processor.fpr[fd] = Float.intBitsToFloat((int) (processor.fpr[fs]));
                    break;
                case 2:
                    processor.fpr[fd] = Float.intBitsToFloat((int) Math.ceil(processor.fpr[fs]));
                    break;
                case 3:
                    processor.fpr[fd] = Float.intBitsToFloat((int) Math.floor(processor.fpr[fs]));
                    break;
                default:
                    processor.fpr[fd] = Float.intBitsToFloat((int) Math.rint(processor.fpr[fs]));
                    break;
            }

        }

        @Override
        public void compile(Processor processor, int insn) {
            int fd = (insn >> 6) & 31;
            int fs = (insn >> 11) & 31;



            processor.load_fpr(fs, false);
            processor.alter_fpr(fd);
            processor.current_bb.emit(
                    "switch (processor.fcr31_rm) {\n" +
                    "case 1:\n" +
                    processor.get_fpr(fd) +
                    " = Float.intBitsToFloat((int) (" +
                    processor.get_fpr(fs) +
                    "));\n" +
                    "break;\n" +
                    "case 2:\n" +
                    processor.get_fpr(fd) +
                    " = Float.intBitsToFloat((int) Math.ceil(" +
                    processor.get_fpr(fs) +
                    "));\n" +
                    "break;\n" +
                    "case 3:\n" +
                    processor.get_fpr(fd) +
                    " = Float.intBitsToFloat((int) Math.floor(" +
                    processor.get_fpr(fs) +
                    "));\n" +
                    "break;\n" +
                    "default:\n" +
                    processor.get_fpr(fd) +
                    " = Float.intBitsToFloat((int) Math.rint(" +
                    processor.get_fpr(fs) +
                    "));\n" +
                    "break;\n" +
                    "}\n");

        }

        @Override
        public String disasm(int address, int insn) {
            int fd = (insn >> 6) & 31;
            int fs = (insn >> 11) & 31;


            return Dis_FDFS("cvt.w.s", fd, fs);
        }
    };
    public static final Instruction C_COND_S = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int fcond = (insn >> 0) & 15;
            int fs = (insn >> 11) & 31;
            int ft = (insn >> 16) & 31;



            float x = processor.fpr[fs];
            float y = processor.fpr[ft];
            boolean unordered = ((fcond & 1) != 0) && (Float.isNaN(x) || Float.isNaN(y));

            if (unordered) {
                // we ignore float exception as it is useless for games
                //if ((fcond & 8) != 0) {
                //}

                processor.fcr31_c = true;
            } else {
                boolean equal = ((fcond & 2) != 0) && (x == y);
                boolean less = ((fcond & 4) != 0) && (x < y);

                processor.fcr31_c = less || equal;
            }

        }

        @Override
        public void compile(Processor processor, int insn) {
            int fcond = (insn >> 0) & 15;
            int fs = (insn >> 11) & 31;
            int ft = (insn >> 16) & 31;



            processor.load_fpr(fs, false);
            processor.load_fpr(ft, false);
            processor.current_bb.emit(
                    "{\n" +
                    "float x = " + processor.get_fpr(fs) + ";" +
                    "float y = " + processor.get_fpr(ft) + ";" +
                    "boolean unordered = ((" + fcond + " & 1) != 0) && (Float.isNaN(x) || Float.isNaN(y));" +
                    "if (unordered) {\n" +
                    "// we ignore float exception as it is useless for games\n" +
                    "//if ((" + fcond + " & 8) != 0) {\n" +
                    "//}\n" +
                    "processor.fcr31_c = true;" +
                    "} else {\n" +
                    "boolean equal = ((" + fcond + " & 2) != 0) && (x == y);" +
                    "boolean less = ((" + fcond + " & 4) != 0) && (x < y);" +
                    "processor.fcr31_c = less || equal;" +
                    "}\n}\n");

        }

        @Override
        public String disasm(int address, int insn) {
            int fcond = (insn >> 0) & 15;
            int fs = (insn >> 11) & 31;
            int ft = (insn >> 16) & 31;


            return Dis_Cconds(fcond, fs, ft);
        }
    };
    public static final Instruction MFC1 = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int fs = (insn >> 11) & 31;
            int rt = (insn >> 16) & 31;



            if (rt != 0) {
                processor.gpr[rt] = Float.floatToRawIntBits(processor.fpr[fs]);
            }

        }

        @Override
        public void compile(Processor processor, int insn) {
            int fs = (insn >> 11) & 31;
            int rt = (insn >> 16) & 31;



            if (rt != 0) {
                if (processor.tracked_fpr[fs].fixed) {
                    processor.fix_gpr(rt, Float.floatToRawIntBits(processor.fpr[fs]));
                } else {
                    processor.load_fpr(fs, false);
                    processor.alter_gpr(rt);
                    processor.current_bb.emit(
                            processor.get_gpr(rt) +
                            " = Float.floatToRawIntBits(" +
                            processor.get_fpr(fs) +
                            ");");
                }
            }

        }

        @Override
        public String disasm(int address, int insn) {
            int fs = (insn >> 11) & 31;
            int rt = (insn >> 16) & 31;


            return Dis_RTFS("mfc1", rt, fs);
        }
    };
    public static final Instruction CFC1 = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int c1cr = (insn >> 11) & 31;
            int rt = (insn >> 16) & 31;



            if (rt != 0) {
                switch (c1cr) {
                    case 0:
                        processor.gpr[rt] = (Processor.fcr0_imp << 8) | (Processor.fcr0_rev);
                        break;

                    case 31:
                        processor.gpr[rt] = (processor.fcr31_fs ? (1 << 24) : 0) | (processor.fcr31_c ? (1 << 23) : 0) | (processor.fcr31_rm & 3);
                        break;
                }
            }

        }

        @Override
        public void compile(Processor processor, int insn) {
            int c1cr = (insn >> 11) & 31;
            int rt = (insn >> 16) & 31;



            if (rt != 0) {
                switch (c1cr) {
                    case 0:
                        processor.alter_gpr(rt);
                        processor.current_bb.emit(
                                processor.get_gpr(rt) +
                                " = (Processor.fcr0_imp << 8) | (Processor.fcr0_rev);");
                        break;

                    case 31:
                        processor.alter_gpr(rt);
                        processor.current_bb.emit(
                                processor.get_gpr(rt) +
                                " = (processor.fcr31_fs ? (1 << 24) : 0) | (processor.fcr31_c ? (1 << 23) : 0) | (processor.fcr31_rm & 3);");
                        break;
                }
            }

        }

        @Override
        public String disasm(int address, int insn) {
            int c1cr = (insn >> 11) & 31;
            int rt = (insn >> 16) & 31;


            return Dis_RTFC("cfc1", rt, c1cr);
        }
    };
    public static final Instruction MTC1 = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int fs = (insn >> 11) & 31;
            int rt = (insn >> 16) & 31;



            processor.fpr[fs] = Float.intBitsToFloat(processor.gpr[rt]);

        }

        @Override
        public void compile(Processor processor, int insn) {
            int fs = (insn >> 11) & 31;
            int rt = (insn >> 16) & 31;



            if (processor.tracked_gpr[rt].fixed) {
                processor.fix_fpr(fs, Float.intBitsToFloat(processor.gpr[rt]));
            } else {
                processor.load_gpr(rt, false);
                processor.alter_fpr(fs);
                processor.current_bb.emit(
                        processor.get_fpr(fs) +
                        " = Float.intBitsToFloat(" +
                        processor.get_gpr(rt) +
                        ");");
            }

        }

        @Override
        public String disasm(int address, int insn) {
            int fs = (insn >> 11) & 31;
            int rt = (insn >> 16) & 31;


            return Dis_RTFS("mtc1", rt, fs);
        }
    };
    public static final Instruction CTC1 = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int c1cr = (insn >> 11) & 31;
            int rt = (insn >> 16) & 31;



            if (c1cr == 31) {
                int bits = processor.gpr[rt] & 0x01800003;
                processor.fcr31_rm = bits & 3;
                bits >>= 23;
                processor.fcr31_fs = (bits > 1);
                processor.fcr31_c = (bits >> 1) == 1;
            }

        }

        @Override
        public void compile(Processor processor, int insn) {
            int c1cr = (insn >> 11) & 31;
            int rt = (insn >> 16) & 31;



            if (c1cr == 31) {
                processor.load_gpr(rt, false);
                processor.current_bb.emit(
                        "{\n" +
                        "int bits = " + processor.get_gpr(rt) + " & 0x01800003;" +
                        "processor.fcr31_rm = bits & 3;" +
                        "bits >>= 23;" +
                        "processor.fcr31_fs = (bits > 1);" +
                        "processor.fcr31_c = (bits >> 1) == 1;" +
                        "}\n");
            }

        }

        @Override
        public String disasm(int address, int insn) {
            int c1cr = (insn >> 11) & 31;
            int rt = (insn >> 16) & 31;


            return Dis_RTFC("ctc1", rt, c1cr);
        }
    };
    public static final Instruction MFC0 = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int c0dr = (insn >> 11) & 31;
            int rt = (insn >> 16) & 31;




        }

        @Override
        public void compile(Processor processor, int insn) {
            int c0dr = (insn >> 11) & 31;
            int rt = (insn >> 16) & 31;




        }

        @Override
        public String disasm(int address, int insn) {
            int c0dr = (insn >> 11) & 31;
            int rt = (insn >> 16) & 31;


            return "Unimplemented MFC0";
        }
    };
    public static final Instruction CFC0 = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int c0cr = (insn >> 11) & 31;
            int rt = (insn >> 16) & 31;




        }

        @Override
        public void compile(Processor processor, int insn) {
            int c0cr = (insn >> 11) & 31;
            int rt = (insn >> 16) & 31;




        }

        @Override
        public String disasm(int address, int insn) {
            int c0cr = (insn >> 11) & 31;
            int rt = (insn >> 16) & 31;


            return "Unimplemented CFC0";
        }
    };
    public static final Instruction MTC0 = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int c0dr = (insn >> 11) & 31;
            int rt = (insn >> 16) & 31;




        }

        @Override
        public void compile(Processor processor, int insn) {
            int c0dr = (insn >> 11) & 31;
            int rt = (insn >> 16) & 31;




        }

        @Override
        public String disasm(int address, int insn) {
            int c0dr = (insn >> 11) & 31;
            int rt = (insn >> 16) & 31;


            return "Unimplemented MTC0";
        }
    };
    public static final Instruction CTC0 = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int c0cr = (insn >> 11) & 31;
            int rt = (insn >> 16) & 31;




        }

        @Override
        public void compile(Processor processor, int insn) {
            int c0cr = (insn >> 11) & 31;
            int rt = (insn >> 16) & 31;




        }

        @Override
        public String disasm(int address, int insn) {
            int c0cr = (insn >> 11) & 31;
            int rt = (insn >> 16) & 31;


            return "Unimplemented CTC0";
        }
    };
    public static final Instruction VADD = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;
            int vt = (insn >> 16) & 127;




        }

        @Override
        public void compile(Processor processor, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;
            int vt = (insn >> 16) & 127;




        }

        @Override
        public String disasm(int address, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;
            int vt = (insn >> 16) & 127;


            return "Unimplemented VADD";
        }
    };
    public static final Instruction VSUB = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;
            int vt = (insn >> 16) & 127;




        }

        @Override
        public void compile(Processor processor, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;
            int vt = (insn >> 16) & 127;




        }

        @Override
        public String disasm(int address, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;
            int vt = (insn >> 16) & 127;


            return "Unimplemented VSUB";
        }
    };
    public static final Instruction VSBN = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;
            int vt = (insn >> 16) & 127;




        }

        @Override
        public void compile(Processor processor, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;
            int vt = (insn >> 16) & 127;




        }

        @Override
        public String disasm(int address, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;
            int vt = (insn >> 16) & 127;


            return "Unimplemented VSBN";
        }
    };
    public static final Instruction VDIV = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;
            int vt = (insn >> 16) & 127;




        }

        @Override
        public void compile(Processor processor, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;
            int vt = (insn >> 16) & 127;




        }

        @Override
        public String disasm(int address, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;
            int vt = (insn >> 16) & 127;


            return "Unimplemented VDIV";
        }
    };
    public static final Instruction VMUL = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;
            int vt = (insn >> 16) & 127;




        }

        @Override
        public void compile(Processor processor, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;
            int vt = (insn >> 16) & 127;




        }

        @Override
        public String disasm(int address, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;
            int vt = (insn >> 16) & 127;


            return "Unimplemented VMUL";
        }
    };
    public static final Instruction VDOT = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;
            int vt = (insn >> 16) & 127;




        }

        @Override
        public void compile(Processor processor, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;
            int vt = (insn >> 16) & 127;




        }

        @Override
        public String disasm(int address, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;
            int vt = (insn >> 16) & 127;


            return "Unimplemented VDOT";
        }
    };
    public static final Instruction VSCL = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;
            int vt = (insn >> 16) & 127;




        }

        @Override
        public void compile(Processor processor, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;
            int vt = (insn >> 16) & 127;




        }

        @Override
        public String disasm(int address, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;
            int vt = (insn >> 16) & 127;


            return "Unimplemented VSCL";
        }
    };
    public static final Instruction VHDP = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;
            int vt = (insn >> 16) & 127;




        }

        @Override
        public void compile(Processor processor, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;
            int vt = (insn >> 16) & 127;




        }

        @Override
        public String disasm(int address, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;
            int vt = (insn >> 16) & 127;


            return "Unimplemented VHDP";
        }
    };
    public static final Instruction VDET = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;
            int vt = (insn >> 16) & 127;




        }

        @Override
        public void compile(Processor processor, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;
            int vt = (insn >> 16) & 127;




        }

        @Override
        public String disasm(int address, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;
            int vt = (insn >> 16) & 127;


            return "Unimplemented VDET";
        }
    };
    public static final Instruction VCRS = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;
            int vt = (insn >> 16) & 127;




        }

        @Override
        public void compile(Processor processor, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;
            int vt = (insn >> 16) & 127;




        }

        @Override
        public String disasm(int address, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;
            int vt = (insn >> 16) & 127;


            return "Unimplemented VCRS";
        }
    };
    public static final Instruction MFV = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int imm7 = (insn >> 0) & 127;
            int rt = (insn >> 16) & 31;




        }

        @Override
        public void compile(Processor processor, int insn) {
            int imm7 = (insn >> 0) & 127;
            int rt = (insn >> 16) & 31;




        }

        @Override
        public String disasm(int address, int insn) {
            int imm7 = (insn >> 0) & 127;
            int rt = (insn >> 16) & 31;


            return "Unimplemented MFV";
        }
    };
    public static final Instruction MFVC = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int imm7 = (insn >> 0) & 127;
            int rt = (insn >> 16) & 31;




        }

        @Override
        public void compile(Processor processor, int insn) {
            int imm7 = (insn >> 0) & 127;
            int rt = (insn >> 16) & 31;




        }

        @Override
        public String disasm(int address, int insn) {
            int imm7 = (insn >> 0) & 127;
            int rt = (insn >> 16) & 31;


            return "Unimplemented MFVC";
        }
    };
    public static final Instruction MTV = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int imm7 = (insn >> 0) & 127;
            int rt = (insn >> 16) & 31;




        }

        @Override
        public void compile(Processor processor, int insn) {
            int imm7 = (insn >> 0) & 127;
            int rt = (insn >> 16) & 31;




        }

        @Override
        public String disasm(int address, int insn) {
            int imm7 = (insn >> 0) & 127;
            int rt = (insn >> 16) & 31;


            return "Unimplemented MTV";
        }
    };
    public static final Instruction MTVC = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int imm7 = (insn >> 0) & 127;
            int rt = (insn >> 16) & 31;




        }

        @Override
        public void compile(Processor processor, int insn) {
            int imm7 = (insn >> 0) & 127;
            int rt = (insn >> 16) & 31;




        }

        @Override
        public String disasm(int address, int insn) {
            int imm7 = (insn >> 0) & 127;
            int rt = (insn >> 16) & 31;


            return "Unimplemented MTVC";
        }
    };
    public static final Instruction BVF = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int imm16 = (insn >> 0) & 65535;
            int imm3 = (insn >> 18) & 7;




        }

        @Override
        public void compile(Processor processor, int insn) {
            int imm16 = (insn >> 0) & 65535;
            int imm3 = (insn >> 18) & 7;




        }

        @Override
        public String disasm(int address, int insn) {
            int imm16 = (insn >> 0) & 65535;
            int imm3 = (insn >> 18) & 7;


            return "Unimplemented BVF";
        }
    };
    public static final Instruction BVT = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int imm16 = (insn >> 0) & 65535;
            int imm3 = (insn >> 18) & 7;




        }

        @Override
        public void compile(Processor processor, int insn) {
            int imm16 = (insn >> 0) & 65535;
            int imm3 = (insn >> 18) & 7;




        }

        @Override
        public String disasm(int address, int insn) {
            int imm16 = (insn >> 0) & 65535;
            int imm3 = (insn >> 18) & 7;


            return "Unimplemented BVF";
        }
    };
    public static final Instruction BVFL = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int imm16 = (insn >> 0) & 65535;
            int imm3 = (insn >> 18) & 7;




        }

        @Override
        public void compile(Processor processor, int insn) {
            int imm16 = (insn >> 0) & 65535;
            int imm3 = (insn >> 18) & 7;




        }

        @Override
        public String disasm(int address, int insn) {
            int imm16 = (insn >> 0) & 65535;
            int imm3 = (insn >> 18) & 7;


            return "Unimplemented BVF";
        }
    };
    public static final Instruction BVTL = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int imm16 = (insn >> 0) & 65535;
            int imm3 = (insn >> 18) & 7;




        }

        @Override
        public void compile(Processor processor, int insn) {
            int imm16 = (insn >> 0) & 65535;
            int imm3 = (insn >> 18) & 7;




        }

        @Override
        public String disasm(int address, int insn) {
            int imm16 = (insn >> 0) & 65535;
            int imm3 = (insn >> 18) & 7;


            return "Unimplemented BVF";
        }
    };
    public static final Instruction VCMP = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int imm3 = (insn >> 0) & 7;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;
            int vt = (insn >> 16) & 127;




        }

        @Override
        public void compile(Processor processor, int insn) {
            int imm3 = (insn >> 0) & 7;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;
            int vt = (insn >> 16) & 127;




        }

        @Override
        public String disasm(int address, int insn) {
            int imm3 = (insn >> 0) & 7;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;
            int vt = (insn >> 16) & 127;


            return "Unimplemented VCMP";
        }
    };
    public static final Instruction VMIN = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;
            int vt = (insn >> 16) & 127;




        }

        @Override
        public void compile(Processor processor, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;
            int vt = (insn >> 16) & 127;




        }

        @Override
        public String disasm(int address, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;
            int vt = (insn >> 16) & 127;


            return "Unimplemented VMIN";
        }
    };
    public static final Instruction VMAX = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;
            int vt = (insn >> 16) & 127;




        }

        @Override
        public void compile(Processor processor, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;
            int vt = (insn >> 16) & 127;




        }

        @Override
        public String disasm(int address, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;
            int vt = (insn >> 16) & 127;


            return "Unimplemented VMAX";
        }
    };
    public static final Instruction VSCMP = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;
            int vt = (insn >> 16) & 127;




        }

        @Override
        public void compile(Processor processor, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;
            int vt = (insn >> 16) & 127;




        }

        @Override
        public String disasm(int address, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;
            int vt = (insn >> 16) & 127;


            return "Unimplemented VSCMP";
        }
    };
    public static final Instruction VSGE = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;
            int vt = (insn >> 16) & 127;




        }

        @Override
        public void compile(Processor processor, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;
            int vt = (insn >> 16) & 127;




        }

        @Override
        public String disasm(int address, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;
            int vt = (insn >> 16) & 127;


            return "Unimplemented VSGE";
        }
    };
    public static final Instruction VSLT = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;
            int vt = (insn >> 16) & 127;




        }

        @Override
        public void compile(Processor processor, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;
            int vt = (insn >> 16) & 127;




        }

        @Override
        public String disasm(int address, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;
            int vt = (insn >> 16) & 127;


            return "Unimplemented VSLT";
        }
    };
    public static final Instruction VMOV = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;




        }

        @Override
        public void compile(Processor processor, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;




        }

        @Override
        public String disasm(int address, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;


            return "Unimplemented VMOV";
        }
    };
    public static final Instruction VABS = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;




        }

        @Override
        public void compile(Processor processor, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;




        }

        @Override
        public String disasm(int address, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;


            return "Unimplemented VABS";
        }
    };
    public static final Instruction VNEG = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;




        }

        @Override
        public void compile(Processor processor, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;




        }

        @Override
        public String disasm(int address, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;


            return "Unimplemented VNEG";
        }
    };
    public static final Instruction VIDT = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int two = (insn >> 15) & 1;




        }

        @Override
        public void compile(Processor processor, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int two = (insn >> 15) & 1;




        }

        @Override
        public String disasm(int address, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int two = (insn >> 15) & 1;


            return "Unimplemented VIDT";
        }
    };
    public static final Instruction VSAT0 = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;




        }

        @Override
        public void compile(Processor processor, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;




        }

        @Override
        public String disasm(int address, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;


            return "Unimplemented VSAT0";
        }
    };
    public static final Instruction VSAT1 = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;




        }

        @Override
        public void compile(Processor processor, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;




        }

        @Override
        public String disasm(int address, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;


            return "Unimplemented VSAT1";
        }
    };
    public static final Instruction VZERO = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int two = (insn >> 15) & 1;




        }

        @Override
        public void compile(Processor processor, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int two = (insn >> 15) & 1;




        }

        @Override
        public String disasm(int address, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int two = (insn >> 15) & 1;


            return "Unimplemented VZERO";
        }
    };
    public static final Instruction VONE = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int two = (insn >> 15) & 1;




        }

        @Override
        public void compile(Processor processor, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int two = (insn >> 15) & 1;




        }

        @Override
        public String disasm(int address, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int two = (insn >> 15) & 1;


            return "Unimplemented VONE";
        }
    };
    public static final Instruction VRCP = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;




        }

        @Override
        public void compile(Processor processor, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;




        }

        @Override
        public String disasm(int address, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;


            return "Unimplemented VRCP";
        }
    };
    public static final Instruction VRSQ = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;




        }

        @Override
        public void compile(Processor processor, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;




        }

        @Override
        public String disasm(int address, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;


            return "Unimplemented VRSQ";
        }
    };
    public static final Instruction VSIN = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;




        }

        @Override
        public void compile(Processor processor, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;




        }

        @Override
        public String disasm(int address, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;


            return "Unimplemented VSIN";
        }
    };
    public static final Instruction VCOS = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;




        }

        @Override
        public void compile(Processor processor, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;




        }

        @Override
        public String disasm(int address, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;


            return "Unimplemented VCOS";
        }
    };
    public static final Instruction VEXP2 = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;




        }

        @Override
        public void compile(Processor processor, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;




        }

        @Override
        public String disasm(int address, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;


            return "Unimplemented VEXP2";
        }
    };
    public static final Instruction VLOG2 = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;




        }

        @Override
        public void compile(Processor processor, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;




        }

        @Override
        public String disasm(int address, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;


            return "Unimplemented VLOG2";
        }
    };
    public static final Instruction VSQRT = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;




        }

        @Override
        public void compile(Processor processor, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;




        }

        @Override
        public String disasm(int address, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;


            return "Unimplemented VSQRT";
        }
    };
    public static final Instruction VASIN = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;




        }

        @Override
        public void compile(Processor processor, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;




        }

        @Override
        public String disasm(int address, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;


            return "Unimplemented VASIN";
        }
    };
    public static final Instruction VNRCP = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;




        }

        @Override
        public void compile(Processor processor, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;




        }

        @Override
        public String disasm(int address, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;


            return "Unimplemented VNRCP";
        }
    };
    public static final Instruction VNSIN = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;




        }

        @Override
        public void compile(Processor processor, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;




        }

        @Override
        public String disasm(int address, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;


            return "Unimplemented VNSIN";
        }
    };
    public static final Instruction VREXP2 = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;




        }

        @Override
        public void compile(Processor processor, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;




        }

        @Override
        public String disasm(int address, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;


            return "Unimplemented VREXP2";
        }
    };
    public static final Instruction VRNDS = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;




        }

        @Override
        public void compile(Processor processor, int insn) {
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;




        }

        @Override
        public String disasm(int address, int insn) {
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;


            return "Unimplemented VRNDS";
        }
    };
    public static final Instruction VRNDI = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int two = (insn >> 15) & 1;




        }

        @Override
        public void compile(Processor processor, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int two = (insn >> 15) & 1;




        }

        @Override
        public String disasm(int address, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int two = (insn >> 15) & 1;


            return "Unimplemented VRNDI";
        }
    };
    public static final Instruction VRNDF1 = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int two = (insn >> 15) & 1;




        }

        @Override
        public void compile(Processor processor, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int two = (insn >> 15) & 1;




        }

        @Override
        public String disasm(int address, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int two = (insn >> 15) & 1;


            return "Unimplemented VRNDF1";
        }
    };
    public static final Instruction VRNDF2 = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int two = (insn >> 15) & 1;




        }

        @Override
        public void compile(Processor processor, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int two = (insn >> 15) & 1;




        }

        @Override
        public String disasm(int address, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int two = (insn >> 15) & 1;


            return "Unimplemented VRNDF2";
        }
    };
    public static final Instruction VF2H = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;




        }

        @Override
        public void compile(Processor processor, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;




        }

        @Override
        public String disasm(int address, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;


            return "Unimplemented VF2H";
        }
    };
    public static final Instruction VH2F = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;




        }

        @Override
        public void compile(Processor processor, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;




        }

        @Override
        public String disasm(int address, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;


            return "Unimplemented VH2F";
        }
    };
    public static final Instruction VSBZ = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;




        }

        @Override
        public void compile(Processor processor, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;




        }

        @Override
        public String disasm(int address, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;


            return "Unimplemented VSBZ";
        }
    };
    public static final Instruction VLGB = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;




        }

        @Override
        public void compile(Processor processor, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;




        }

        @Override
        public String disasm(int address, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;


            return "Unimplemented VLGB";
        }
    };
    public static final Instruction VUC2I = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;




        }

        @Override
        public void compile(Processor processor, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;




        }

        @Override
        public String disasm(int address, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;


            return "Unimplemented VUC2I";
        }
    };
    public static final Instruction VC2I = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;




        }

        @Override
        public void compile(Processor processor, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;




        }

        @Override
        public String disasm(int address, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;


            return "Unimplemented VC2I";
        }
    };
    public static final Instruction VUS2I = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;




        }

        @Override
        public void compile(Processor processor, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;




        }

        @Override
        public String disasm(int address, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;


            return "Unimplemented VUS2I";
        }
    };
    public static final Instruction VS2I = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;




        }

        @Override
        public void compile(Processor processor, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;




        }

        @Override
        public String disasm(int address, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;


            return "Unimplemented VS2I";
        }
    };
    public static final Instruction VI2UC = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;




        }

        @Override
        public void compile(Processor processor, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;




        }

        @Override
        public String disasm(int address, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;


            return "Unimplemented VI2UC";
        }
    };
    public static final Instruction VI2C = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;




        }

        @Override
        public void compile(Processor processor, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;




        }

        @Override
        public String disasm(int address, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;


            return "Unimplemented VI2C";
        }
    };
    public static final Instruction VI2US = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;




        }

        @Override
        public void compile(Processor processor, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;




        }

        @Override
        public String disasm(int address, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;


            return "Unimplemented VI2US";
        }
    };
    public static final Instruction VI2S = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;




        }

        @Override
        public void compile(Processor processor, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;




        }

        @Override
        public String disasm(int address, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;


            return "Unimplemented VI2S";
        }
    };
    public static final Instruction VSRT1 = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;




        }

        @Override
        public void compile(Processor processor, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;




        }

        @Override
        public String disasm(int address, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;


            return "Unimplemented VSRT1";
        }
    };
    public static final Instruction VSRT2 = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;




        }

        @Override
        public void compile(Processor processor, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;




        }

        @Override
        public String disasm(int address, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;


            return "Unimplemented VSRT2";
        }
    };
    public static final Instruction VBFY1 = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;




        }

        @Override
        public void compile(Processor processor, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;




        }

        @Override
        public String disasm(int address, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;


            return "Unimplemented VBFY1";
        }
    };
    public static final Instruction VBFY2 = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;




        }

        @Override
        public void compile(Processor processor, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;




        }

        @Override
        public String disasm(int address, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;


            return "Unimplemented VBFY2";
        }
    };
    public static final Instruction VOCP = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;




        }

        @Override
        public void compile(Processor processor, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;




        }

        @Override
        public String disasm(int address, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;


            return "Unimplemented VOCP";
        }
    };
    public static final Instruction VSOCP = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;




        }

        @Override
        public void compile(Processor processor, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;




        }

        @Override
        public String disasm(int address, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;


            return "Unimplemented VSOCP";
        }
    };
    public static final Instruction VFAD = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;




        }

        @Override
        public void compile(Processor processor, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;




        }

        @Override
        public String disasm(int address, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;


            return "Unimplemented VFAD";
        }
    };
    public static final Instruction VAVG = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;




        }

        @Override
        public void compile(Processor processor, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;




        }

        @Override
        public String disasm(int address, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;


            return "Unimplemented VAVG";
        }
    };
    public static final Instruction VSRT3 = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;




        }

        @Override
        public void compile(Processor processor, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;




        }

        @Override
        public String disasm(int address, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;


            return "Unimplemented VSRT3";
        }
    };
    public static final Instruction VSRT4 = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;




        }

        @Override
        public void compile(Processor processor, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;




        }

        @Override
        public String disasm(int address, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;


            return "Unimplemented VSRT4";
        }
    };
    public static final Instruction VMFVC = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int vd = (insn >> 0) & 127;
            int imm8 = (insn >> 8) & 255;




        }

        @Override
        public void compile(Processor processor, int insn) {
            int vd = (insn >> 0) & 127;
            int imm8 = (insn >> 8) & 255;




        }

        @Override
        public String disasm(int address, int insn) {
            int vd = (insn >> 0) & 127;
            int imm8 = (insn >> 8) & 255;


            return "Unimplemented VMFVC";
        }
    };
    public static final Instruction VMTVC = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int imm8 = (insn >> 0) & 255;
            int vs = (insn >> 8) & 127;




        }

        @Override
        public void compile(Processor processor, int insn) {
            int imm8 = (insn >> 0) & 255;
            int vs = (insn >> 8) & 127;




        }

        @Override
        public String disasm(int address, int insn) {
            int imm8 = (insn >> 0) & 255;
            int vs = (insn >> 8) & 127;


            return "Unimplemented VMTVC";
        }
    };
    public static final Instruction VT4444 = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;




        }

        @Override
        public void compile(Processor processor, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;




        }

        @Override
        public String disasm(int address, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;


            return "Unimplemented VT4444";
        }
    };
    public static final Instruction VT5551 = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;




        }

        @Override
        public void compile(Processor processor, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;




        }

        @Override
        public String disasm(int address, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;


            return "Unimplemented VT5551";
        }
    };
    public static final Instruction VT5650 = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;




        }

        @Override
        public void compile(Processor processor, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;




        }

        @Override
        public String disasm(int address, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;


            return "Unimplemented VT5650";
        }
    };
    public static final Instruction VCST = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int two = (insn >> 15) & 1;




        }

        @Override
        public void compile(Processor processor, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int two = (insn >> 15) & 1;




        }

        @Override
        public String disasm(int address, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int two = (insn >> 15) & 1;


            return "Unimplemented VCST";
        }
    };
    public static final Instruction VF2IN = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;




        }

        @Override
        public void compile(Processor processor, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;




        }

        @Override
        public String disasm(int address, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;


            return "Unimplemented VF2IN";
        }
    };
    public static final Instruction VF2IZ = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;




        }

        @Override
        public void compile(Processor processor, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;




        }

        @Override
        public String disasm(int address, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;


            return "Unimplemented VF2IZ";
        }
    };
    public static final Instruction VF2IU = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;




        }

        @Override
        public void compile(Processor processor, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;




        }

        @Override
        public String disasm(int address, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;


            return "Unimplemented VF2IU";
        }
    };
    public static final Instruction VF2ID = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;




        }

        @Override
        public void compile(Processor processor, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;




        }

        @Override
        public String disasm(int address, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;


            return "Unimplemented VF2ID";
        }
    };
    public static final Instruction VI2F = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;




        }

        @Override
        public void compile(Processor processor, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;




        }

        @Override
        public String disasm(int address, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;


            return "Unimplemented VI2F";
        }
    };
    public static final Instruction VCMOVT = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;
            int imm3 = (insn >> 16) & 7;




        }

        @Override
        public void compile(Processor processor, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;
            int imm3 = (insn >> 16) & 7;




        }

        @Override
        public String disasm(int address, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;
            int imm3 = (insn >> 16) & 7;


            return "Unimplemented VCMOVT";
        }
    };
    public static final Instruction VCMOVF = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;
            int imm3 = (insn >> 16) & 7;




        }

        @Override
        public void compile(Processor processor, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;
            int imm3 = (insn >> 16) & 7;




        }

        @Override
        public String disasm(int address, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;
            int imm3 = (insn >> 16) & 7;


            return "Unimplemented VCMOVF";
        }
    };
    public static final Instruction VWBN = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;
            int imm8 = (insn >> 16) & 255;




        }

        @Override
        public void compile(Processor processor, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;
            int imm8 = (insn >> 16) & 255;




        }

        @Override
        public String disasm(int address, int insn) {
            int vd = (insn >> 0) & 127;
            int one = (insn >> 7) & 1;
            int vs = (insn >> 8) & 127;
            int two = (insn >> 15) & 1;
            int imm8 = (insn >> 16) & 255;


            return "Unimplemented VWBN";
        }
    };
    public static final Instruction VPFXS = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int swzx = (insn >> 0) & 3;
            int swzy = (insn >> 2) & 3;
            int swzz = (insn >> 4) & 3;
            int swzw = (insn >> 6) & 3;
            int absx = (insn >> 8) & 1;
            int absy = (insn >> 9) & 1;
            int absz = (insn >> 10) & 1;
            int absw = (insn >> 11) & 1;
            int cstx = (insn >> 12) & 1;
            int csty = (insn >> 13) & 1;
            int cstz = (insn >> 14) & 1;
            int cstw = (insn >> 15) & 1;
            int negx = (insn >> 16) & 1;
            int negy = (insn >> 17) & 1;
            int negz = (insn >> 18) & 1;
            int negw = (insn >> 19) & 1;




        }

        @Override
        public void compile(Processor processor, int insn) {
            int swzx = (insn >> 0) & 3;
            int swzy = (insn >> 2) & 3;
            int swzz = (insn >> 4) & 3;
            int swzw = (insn >> 6) & 3;
            int absx = (insn >> 8) & 1;
            int absy = (insn >> 9) & 1;
            int absz = (insn >> 10) & 1;
            int absw = (insn >> 11) & 1;
            int cstx = (insn >> 12) & 1;
            int csty = (insn >> 13) & 1;
            int cstz = (insn >> 14) & 1;
            int cstw = (insn >> 15) & 1;
            int negx = (insn >> 16) & 1;
            int negy = (insn >> 17) & 1;
            int negz = (insn >> 18) & 1;
            int negw = (insn >> 19) & 1;




        }

        @Override
        public String disasm(int address, int insn) {
            int swzx = (insn >> 0) & 3;
            int swzy = (insn >> 2) & 3;
            int swzz = (insn >> 4) & 3;
            int swzw = (insn >> 6) & 3;
            int absx = (insn >> 8) & 1;
            int absy = (insn >> 9) & 1;
            int absz = (insn >> 10) & 1;
            int absw = (insn >> 11) & 1;
            int cstx = (insn >> 12) & 1;
            int csty = (insn >> 13) & 1;
            int cstz = (insn >> 14) & 1;
            int cstw = (insn >> 15) & 1;
            int negx = (insn >> 16) & 1;
            int negy = (insn >> 17) & 1;
            int negz = (insn >> 18) & 1;
            int negw = (insn >> 19) & 1;


            return "Unimplemented VPFXS";
        }
    };
    public static final Instruction VPFXT = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int swzx = (insn >> 0) & 3;
            int swzy = (insn >> 2) & 3;
            int swzz = (insn >> 4) & 3;
            int swzw = (insn >> 6) & 3;
            int absx = (insn >> 8) & 1;
            int absy = (insn >> 9) & 1;
            int absz = (insn >> 10) & 1;
            int absw = (insn >> 11) & 1;
            int cstx = (insn >> 12) & 1;
            int csty = (insn >> 13) & 1;
            int cstz = (insn >> 14) & 1;
            int cstw = (insn >> 15) & 1;
            int negx = (insn >> 16) & 1;
            int negy = (insn >> 17) & 1;
            int negz = (insn >> 18) & 1;
            int negw = (insn >> 19) & 1;




        }

        @Override
        public void compile(Processor processor, int insn) {
            int swzx = (insn >> 0) & 3;
            int swzy = (insn >> 2) & 3;
            int swzz = (insn >> 4) & 3;
            int swzw = (insn >> 6) & 3;
            int absx = (insn >> 8) & 1;
            int absy = (insn >> 9) & 1;
            int absz = (insn >> 10) & 1;
            int absw = (insn >> 11) & 1;
            int cstx = (insn >> 12) & 1;
            int csty = (insn >> 13) & 1;
            int cstz = (insn >> 14) & 1;
            int cstw = (insn >> 15) & 1;
            int negx = (insn >> 16) & 1;
            int negy = (insn >> 17) & 1;
            int negz = (insn >> 18) & 1;
            int negw = (insn >> 19) & 1;




        }

        @Override
        public String disasm(int address, int insn) {
            int swzx = (insn >> 0) & 3;
            int swzy = (insn >> 2) & 3;
            int swzz = (insn >> 4) & 3;
            int swzw = (insn >> 6) & 3;
            int absx = (insn >> 8) & 1;
            int absy = (insn >> 9) & 1;
            int absz = (insn >> 10) & 1;
            int absw = (insn >> 11) & 1;
            int cstx = (insn >> 12) & 1;
            int csty = (insn >> 13) & 1;
            int cstz = (insn >> 14) & 1;
            int cstw = (insn >> 15) & 1;
            int negx = (insn >> 16) & 1;
            int negy = (insn >> 17) & 1;
            int negz = (insn >> 18) & 1;
            int negw = (insn >> 19) & 1;


            return "Unimplemented VPFXT";
        }
    };
    public static final Instruction VPFXD = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int satx = (insn >> 0) & 3;
            int saty = (insn >> 2) & 3;
            int satz = (insn >> 4) & 3;
            int satw = (insn >> 6) & 3;
            int mskx = (insn >> 8) & 1;
            int msky = (insn >> 9) & 1;
            int mskz = (insn >> 10) & 1;
            int mskw = (insn >> 11) & 1;




        }

        @Override
        public void compile(Processor processor, int insn) {
            int satx = (insn >> 0) & 3;
            int saty = (insn >> 2) & 3;
            int satz = (insn >> 4) & 3;
            int satw = (insn >> 6) & 3;
            int mskx = (insn >> 8) & 1;
            int msky = (insn >> 9) & 1;
            int mskz = (insn >> 10) & 1;
            int mskw = (insn >> 11) & 1;




        }

        @Override
        public String disasm(int address, int insn) {
            int satx = (insn >> 0) & 3;
            int saty = (insn >> 2) & 3;
            int satz = (insn >> 4) & 3;
            int satw = (insn >> 6) & 3;
            int mskx = (insn >> 8) & 1;
            int msky = (insn >> 9) & 1;
            int mskz = (insn >> 10) & 1;
            int mskw = (insn >> 11) & 1;


            return "Unimplemented VPFXD";
        }
    };
    public static final Instruction VIIM = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int imm16 = (insn >> 0) & 65535;
            int vd = (insn >> 16) & 127;




        }

        @Override
        public void compile(Processor processor, int insn) {
            int imm16 = (insn >> 0) & 65535;
            int vd = (insn >> 16) & 127;




        }

        @Override
        public String disasm(int address, int insn) {
            int imm16 = (insn >> 0) & 65535;
            int vd = (insn >> 16) & 127;


            return "Unimplemented VIIM";
        }
    };
    public static final Instruction VFIM = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
            int imm16 = (insn >> 0) & 65535;
            int vd = (insn >> 16) & 127;




        }

        @Override
        public void compile(Processor processor, int insn) {
            int imm16 = (insn >> 0) & 65535;
            int vd = (insn >> 16) & 127;




        }

        @Override
        public String disasm(int address, int insn) {
            int imm16 = (insn >> 0) & 65535;
            int vd = (insn >> 16) & 127;


            return "Unimplemented VFIM";
        }
    };
}
