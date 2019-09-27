/*
 * ModLauncher - for launching Java programs with in-flight transformation ability.
 *
 *     Copyright (C) 2017-2019 cpw
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Lesser General Public License as published by
 *     the Free Software Foundation, version 3 of the License.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Lesser General Public License for more details.
 *
 *     You should have received a copy of the GNU Lesser General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package cpw.mods.modlauncher.api;

import java.util.List;

public interface ITransformerVotingContext {
    /**
     * @return The class name being transformed
     */
    String getClassName();

    /**
     * @return If the class already existed
     */
    boolean doesClassExist();

    /**
     * @return The initial sha256 checksum of the class bytes.
     */
    byte[] getInitialClassSha256();

    /**
     * @return The activities already performed on this class. This list is read only, but will change as activities happen.
     */
    List<ITransformerActivity> getAuditActivities();

    String getReason();

    /**
     * Return the result of applying the supplied field predicate to the current field node.
     * Can only be used on a Field target.
     *
     * @param fieldPredicate The field predicate
     * @return true if the predicate passed
     */
    boolean applyFieldPredicate(FieldPredicate fieldPredicate);

    /**
     * Return the result of applying the supplied method predicate to the current method node.
     * Can only be used on a Method target.
     *
     * @param methodPredicate The method predicate
     * @return true if the predicate passed
     */
    boolean applyMethodPredicate(MethodPredicate methodPredicate);

    /**
     * Return the result of applying the supplied class predicate to the current class node.
     * Can only be used on a Class target.
     *
     * @param classPredicate The class predicate
     * @return true if the predicate passed
     */
    boolean applyClassPredicate(ClassPredicate classPredicate);

    /**
     * Return the result of applying the supplied instruction predicate to the current method node.
     * Can only be used on a Method target.
     *
     * @param insnPredicate The insn predicate
     * @return true if the predicate passed
     */
    boolean applyInstructionPredicate(InsnPredicate insnPredicate);

    interface FieldPredicate {
        boolean test(final int access, final String name, final String descriptor, final String signature, final Object value);
    }

    interface MethodPredicate {
        boolean test(final int access, final String name, final String descriptor, final String signature, final String[] exceptions);
    }

    interface ClassPredicate {
        boolean test(final int version, final int access, final String name, final String signature, final String superName, final String[] interfaces);
    }

    interface InsnPredicate {
        boolean test(final int insnCount, final int opcode, Object... args);
    }
}
