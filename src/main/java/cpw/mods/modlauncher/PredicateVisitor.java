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

package cpw.mods.modlauncher;

import cpw.mods.modlauncher.api.ITransformerVotingContext;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class PredicateVisitor extends ClassVisitor {

    private ITransformerVotingContext.MethodPredicate methodPredicate;
    private ITransformerVotingContext.FieldPredicate fieldPredicate;
    private ITransformerVotingContext.ClassPredicate classPredicate;
    private boolean result;

    PredicateVisitor(final ITransformerVotingContext.FieldPredicate fieldPredicate) {
        super(TransformerClassWriter.ASM_VERSION);
        this.fieldPredicate = fieldPredicate;
    }

    PredicateVisitor(final ITransformerVotingContext.MethodPredicate methodPredicate) {
        super(TransformerClassWriter.ASM_VERSION);
        this.methodPredicate = methodPredicate;
    }

    PredicateVisitor(final ITransformerVotingContext.ClassPredicate classPredicate) {
        super(TransformerClassWriter.ASM_VERSION);
        this.classPredicate = classPredicate;
    }

    boolean getResult() {
        return result;
    }

    @Override
    public FieldVisitor visitField(final int access, final String name, final String descriptor, final String signature, final Object value) {
        result = fieldPredicate == null || fieldPredicate.test(access, name, descriptor, signature, value);
        return null;
    }

    @Override
    public MethodVisitor visitMethod(final int access, final String name, final String descriptor, final String signature, final String[] exceptions) {
        result = methodPredicate == null || methodPredicate.test(access, name, descriptor, signature, exceptions);
        return null;
    }

    @Override
    public void visit(final int version, final int access, final String name, final String signature, final String superName, final String[] interfaces) {
        result = classPredicate == null || classPredicate.test(version, access, name, signature, superName, interfaces);
    }

}
