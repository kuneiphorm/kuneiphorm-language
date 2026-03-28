package org.kuneiphorm.language.grammar;

/**
 * A symbol in a grammar production: either a {@link Terminal} or a {@link Variable}.
 *
 * <p>Symbols serve as the unit label type for {@link org.kuneiphorm.daedalus.core.Expression}{@code
 * <Symbol<L>>} trees that represent grammar production bodies.
 *
 * @param <L> the terminal label type
 * @author Florent Guille
 * @since 0.1.0
 */
public sealed interface Symbol<L> permits Terminal, Variable {}
