package no.spk.felles.tidsserie.batch.main;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.joining;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collection;
import java.util.HashSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.read.ListAppender;
import org.assertj.core.api.AbstractCharSequenceAssert;
import org.assertj.core.api.ListAssert;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.LoggerFactory;

/**
 * Klasse som kan brukes som class-rule i JUnit-tester for å sjekke etter spesifikke logg-hendelser.
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public class LogbackVerifier implements BeforeEachCallback, AfterEachCallback {
    private ListAppender<ILoggingEvent> appender;

    @Override
    public void beforeEach(ExtensionContext context) {
        appender = new ListAppender<>();
        appender.setName("MOCK");
        appender.start();

        final Logger rootLogger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        rootLogger.detachAndStopAllAppenders();
        rootLogger.addAppender(this.appender);
    }

    @Override
    public void afterEach(ExtensionContext ctx)  {
        final LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        try {
            final JoranConfigurator configurator = new JoranConfigurator();
            configurator.setContext(context);
            context.reset();
            configurator.doConfigure(LogbackVerifier.class.getClassLoader().getResourceAsStream("logback-test.xml"));
        } catch (final JoranException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Verifiserer loggmeldinger på angitt nivå.
     *
     * @param level alle loggnivå som det skal skjekkes meldinger for
     * @return en asserter som inneholder en liste med den tekstlige meldingen til logg-hendelsene på angitt nivå.
     */
    public ListAssert<String> assertMessagesWithLevel(final Level... level) {
        final HashSet<Level> tmp = new HashSet<>(asList(level));
        return assertThat(
                events()
                        .filter(e -> tmp.contains(e.getLevel()))
                        .map(ILoggingEvent::getFormattedMessage)
                        .toList()
        )
                .as(
                        "messages with level %s",
                        tmp
                                .stream()
                                .map(Level::toString)
                                .collect(joining(" or "))
                );
    }

    /**
     * Verifiserer exception-meldinger knyttet til angitte exception-klasser.
     *
     * @param expectedExeptions alle typene til feil som det skal skjekkes meldinger for
     * @return en asserter som inneholder en liste med den tekstlige meldingen til feilmeldingen for angitt exception.
     */
    public final ListAssert<String> assertMessagesForExceptions(final Collection<Class<? extends Exception>> expectedExeptions) {
        return assertThat(
                events()
                        .map(ILoggingEvent::getThrowableProxy)
                        .filter(t -> isSubtypeOf(t, expectedExeptions))
                        .map(IThrowableProxy::getMessage)
                        .toList()
        )
                .as(
                        "exceptions with subtype of %s",
                        expectedExeptions
                                .stream()
                                .map(Class::getName)
                                .collect(joining(" or "))
                );
    }

    public final AbstractCharSequenceAssert<?, String> assertMessagesAsString() {
        return assertThat(events()
                .map(ILoggingEvent::getFormattedMessage)
                .collect(Collectors.joining("\n")));
    }

    public Stream<ILoggingEvent> events() {
        return appender.list.stream();
    }

    private void reset() {
        appender.list.clear();
    }

    private boolean isSubtypeOf(IThrowableProxy throwable, final Collection<Class<? extends Exception>> expectedExeptions) {
        try {
            final Class<?> type = Class.forName(throwable.getClassName());
            return expectedExeptions.stream().anyMatch(t -> t.isAssignableFrom(type));
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }


}
