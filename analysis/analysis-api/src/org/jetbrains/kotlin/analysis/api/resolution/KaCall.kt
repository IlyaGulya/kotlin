/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.resolution

import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeOwner
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.validityAsserted
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.signatures.KaCallableSignature
import org.jetbrains.kotlin.analysis.api.signatures.KaFunctionSignature
import org.jetbrains.kotlin.analysis.api.signatures.KaVariableSignature
import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaConstructorSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaFunctionSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaNamedFunctionSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaTypeParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaValueParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaVariableSymbol
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.psi.KtExpression

/**
 * A call to a function, a simple/compound access to a property, or a simple/compound access through `get` and `set` convention.
 */
public sealed interface KaCall : KaLifetimeOwner

/**
 * A call to a function, or a simple/compound access to a property.
 */
public sealed class KaCallableMemberCall<S : KaCallableSymbol, C : KaCallableSignature<S>> : KaCall {
    public abstract val partiallyAppliedSymbol: KaPartiallyAppliedSymbol<S, C>

    /**
     * This map returns inferred type arguments. If the type placeholders was used, actual inferred type will be used as a value.
     * Keys for this map is from the set [partiallyAppliedSymbol].signature.typeParameters.
     * In case of resolution or inference error could return empty map.
     */
    public abstract val typeArgumentsMapping: Map<KaTypeParameterSymbol, KaType>
}

public val <S : KaCallableSymbol, C : KaCallableSignature<S>> KaCallableMemberCall<S, C>.symbol: S get() = partiallyAppliedSymbol.symbol

public sealed class KaFunctionCall<S : KaFunctionSymbol>(
    argumentMapping: LinkedHashMap<KtExpression, KaVariableSignature<KaValueParameterSymbol>>,
) : KaCallableMemberCall<S, KaFunctionSignature<S>>() {

    /**
     * The mapping from argument to parameter declaration. In case of vararg parameters, multiple arguments may be mapped to the same
     * [KaValueParameterSymbol].
     */
    public val argumentMapping: LinkedHashMap<KtExpression, KaVariableSignature<KaValueParameterSymbol>>
            by validityAsserted(argumentMapping)
}

/**
 * A call to a function.
 */
public class KaSimpleFunctionCall @KaImplementationDetail constructor(
    partiallyAppliedSymbol: KaPartiallyAppliedFunctionSymbol<KaFunctionSymbol>,
    argumentMapping: LinkedHashMap<KtExpression, KaVariableSignature<KaValueParameterSymbol>>,
    typeArgumentsMapping: Map<KaTypeParameterSymbol, KaType>,
    isImplicitInvoke: Boolean,
) : KaFunctionCall<KaFunctionSymbol>(argumentMapping) {
    private val backingPartiallyAppliedSymbol: KaPartiallyAppliedFunctionSymbol<KaFunctionSymbol> = partiallyAppliedSymbol

    override val token: KaLifetimeToken get() = backingPartiallyAppliedSymbol.token

    /**
     * The function and receivers for this call.
     */
    override val partiallyAppliedSymbol: KaPartiallyAppliedFunctionSymbol<KaFunctionSymbol> get() = withValidityAssertion { backingPartiallyAppliedSymbol }

    override val typeArgumentsMapping: Map<KaTypeParameterSymbol, KaType> by validityAsserted(typeArgumentsMapping)

    /**
     * Whether this function call is an implicit invoke call on a value that has an `invoke` member function. See
     * https://kotlinlang.org/docs/operator-overloading.html#invoke-operator for more details.
     */
    public val isImplicitInvoke: Boolean by validityAsserted(isImplicitInvoke)
}

/**
 * A call to an annotation. For example
 * ```
 * @Deprecated("foo") // call to annotation constructor with single argument `"foo"`.
 * fun foo() {}
 * ```
 */
public class KaAnnotationCall @KaImplementationDetail constructor(
    partiallyAppliedSymbol: KaPartiallyAppliedFunctionSymbol<KaConstructorSymbol>,
    argumentMapping: LinkedHashMap<KtExpression, KaVariableSignature<KaValueParameterSymbol>>,
) : KaFunctionCall<KaConstructorSymbol>(argumentMapping) {
    private val backingPartiallyAppliedSymbol: KaPartiallyAppliedFunctionSymbol<KaConstructorSymbol> = partiallyAppliedSymbol

    override val token: KaLifetimeToken get() = backingPartiallyAppliedSymbol.token

    /**
     * The function and receivers for this call.
     */
    override val partiallyAppliedSymbol: KaPartiallyAppliedFunctionSymbol<KaConstructorSymbol> get() = withValidityAssertion { backingPartiallyAppliedSymbol }

    override val typeArgumentsMapping: Map<KaTypeParameterSymbol, KaType> get() = withValidityAssertion { emptyMap() }
}

/**
 * A delegated call to constructors. For example
 * ```
 * open class SuperClass(i: Int)
 * class SubClass1: SuperClass(1) // a call to constructor of `SuperClass` with single argument `1`
 * class SubClass2 : SuperClass {
 *   constructor(i: Int): super(i) {} // a call to constructor of `SuperClass` with single argument `i`
 *   constructor(): this(2) {} // a call to constructor of `SubClass2` with single argument `2`.
 * }
 * ```
 */
public class KaDelegatedConstructorCall @KaImplementationDetail constructor(
    partiallyAppliedSymbol: KaPartiallyAppliedFunctionSymbol<KaConstructorSymbol>,
    kind: Kind,
    argumentMapping: LinkedHashMap<KtExpression, KaVariableSignature<KaValueParameterSymbol>>,
    typeArgumentsMapping: Map<KaTypeParameterSymbol, KaType>,
) : KaFunctionCall<KaConstructorSymbol>(argumentMapping) {
    private val backingPartiallyAppliedSymbol: KaPartiallyAppliedFunctionSymbol<KaConstructorSymbol> = partiallyAppliedSymbol

    override val token: KaLifetimeToken get() = backingPartiallyAppliedSymbol.token

    /**
     * The function and receivers for this call.
     */
    override val partiallyAppliedSymbol: KaPartiallyAppliedFunctionSymbol<KaConstructorSymbol> get() = withValidityAssertion { backingPartiallyAppliedSymbol }

    override val typeArgumentsMapping: Map<KaTypeParameterSymbol, KaType> by validityAsserted(typeArgumentsMapping)

    public val kind: Kind by validityAsserted(kind)

    public enum class Kind { SUPER_CALL, THIS_CALL }
}

/**
 * An access to variables (including properties).
 */
public sealed class KaVariableAccessCall : KaCallableMemberCall<KaVariableSymbol, KaVariableSignature<KaVariableSymbol>>()

/**
 * A simple read or write to a variable or property.
 */
public class KaSimpleVariableAccessCall @KaImplementationDetail constructor(
    partiallyAppliedSymbol: KaPartiallyAppliedVariableSymbol<KaVariableSymbol>,
    typeArgumentsMapping: Map<KaTypeParameterSymbol, KaType>,
    simpleAccess: KaSimpleVariableAccess,
) : KaVariableAccessCall() {
    private val backingPartiallyAppliedSymbol: KaPartiallyAppliedVariableSymbol<KaVariableSymbol> = partiallyAppliedSymbol

    override val token: KaLifetimeToken get() = backingPartiallyAppliedSymbol.token

    override val partiallyAppliedSymbol: KaPartiallyAppliedVariableSymbol<KaVariableSymbol> get() = withValidityAssertion { backingPartiallyAppliedSymbol }

    override val typeArgumentsMapping: Map<KaTypeParameterSymbol, KaType> by validityAsserted(typeArgumentsMapping)

    /**
     * The type of access to this property.
     */
    public val simpleAccess: KaSimpleVariableAccess by validityAsserted(simpleAccess)
}

public interface KaCompoundAccessCall {
    /**
     * The type of this compound access.
     */
    public val compoundAccess: KaCompoundOperation
}

/**
 * Compound access of a mutable variable.
 * For example:
 * ```kotlin
 * fun test() {
 *   var i = 0
 *   i += 1
 *   // variablePartiallyAppliedSymbol: {
 *   //   symbol: `i`
 *   //   dispatchReceiver: null
 *   //   extensionReceiver: null
 *   // }
 *   // accessType: OpAssign {
 *   //   kind: PLUS
 *   //   operand: 1
 *   //   operationSymbol: Int.plus()
 *   // }
 *
 *   i++
 *   // variablePartiallyAppliedSymbol: {
 *   //   symbol: `i`
 *   //   dispatchReceiver: null
 *   //   extensionReceiver: null
 *   // }
 *   // accessType: IncDec {
 *   //   kind: INC
 *   //   precedence: POSTFIX
 *   //   operationSymbol: Int.inc()
 *   // }
 * }
 * ```
 * Note that if the variable has a `<op>Assign` operator, then it's represented as a simple `KaFunctionCall`.
 * For example,
 * ```kotlin
 * fun test(m: MutableList<String>) {
 *   m += "a" // A simple `KaFunctionCall` to `MutableList.plusAssign`, not a `KaVariableAccessCall`. However, the dispatch receiver of this
 *            // call, `m`, is a simple read access represented as a `KaVariableAccessCall`
 * }
 * ```
 */
public interface KaCompoundVariableAccessCall : KaCall, KaCompoundAccessCall {
    /**
     * Represents a symbol of the mutated variable.
     */
    public val variablePartiallyAppliedSymbol: KaPartiallyAppliedVariableSymbol<KaVariableSymbol>

    @Deprecated("Use 'variablePartiallyAppliedSymbol' instead", ReplaceWith("variablePartiallyAppliedSymbol"))
    public val partiallyAppliedSymbol: KaPartiallyAppliedVariableSymbol<KaVariableSymbol>
        get() = variablePartiallyAppliedSymbol
}

/**
 * A compound access using the array access convention. For example,
 * ```
 * fun test(m: MutableMap<String, String>) {
 *   m["a"] += "b"
 *   // indexArguments: ["a"]
 *   // getPartiallyAppliedSymbol: {
 *   //   symbol: MutableMap.get()
 *   //   dispatchReceiver: `m`
 *   //   extensionReceiver: null
 *   // }
 *   // setPartiallyAppliedSymbol: {
 *   //   symbol: MutableMap.set()
 *   //   dispatchReceiver: `m`
 *   //   extensionReceiver: null
 *   // }
 *   // accessType: OpAssign {
 *   //   kind: PLUS
 *   //   operand: "b"
 *   //   operationSymbol: String?.plus()
 *   // }
 * }
 * ```
 * Such a call always involve both calls to `get` and `set` functions. With the example above, a call to `String?.plus` is sandwiched
 * between `get` and `set` call to compute the new value passed to `set`.
 *
 * Note that simple access using the array access convention is not captured by this class. For example, assuming `ThrowingMap` throws
 * in case of absent key instead of returning `null`,
 * ```
 * fun test(m: ThrowingMap<String, MutableList<String>>) {
 *   m["a"] += "b"
 * }
 * ```
 * The above call is represented as a simple `KaFunctionCall` to `MutableList.plusAssign`, with the dispatch receiver referencing the
 * `m["a"]`, which is again a simple `KaFunctionCall` to `ThrowingMap.get`.
 */
public class KaCompoundArrayAccessCall @KaImplementationDetail constructor(
    compoundAccess: KaCompoundOperation,
    indexArguments: List<KtExpression>,
    getPartiallyAppliedSymbol: KaPartiallyAppliedFunctionSymbol<KaNamedFunctionSymbol>,
    setPartiallyAppliedSymbol: KaPartiallyAppliedFunctionSymbol<KaNamedFunctionSymbol>,
) : KaCall, KaCompoundAccessCall {
    private val backingCompoundAccess: KaCompoundOperation = compoundAccess

    override val token: KaLifetimeToken get() = backingCompoundAccess.token

    override val compoundAccess: KaCompoundOperation get() = withValidityAssertion { backingCompoundAccess }

    public val indexArguments: List<KtExpression> by validityAsserted(indexArguments)

    /**
     * The `get` function that's invoked when reading values corresponding to the given [indexArguments].
     */
    public val getPartiallyAppliedSymbol: KaPartiallyAppliedFunctionSymbol<KaNamedFunctionSymbol> by validityAsserted(getPartiallyAppliedSymbol)

    /**
     * The `set` function that's invoked when writing values corresponding to the given [indexArguments] and computed value from the
     * operation.
     */
    public val setPartiallyAppliedSymbol: KaPartiallyAppliedFunctionSymbol<KaNamedFunctionSymbol> by validityAsserted(setPartiallyAppliedSymbol)
}
