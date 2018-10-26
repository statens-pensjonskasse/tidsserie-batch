package no.spk.felles.tidsserie.batch.main;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.joran.spi.JoranException;
import org.assertj.core.api.AbstractCharSequenceAssert;
import org.assertj.core.api.AbstractListAssert;
import org.junit.rules.ExternalResource;
import org.mockito.Mock;
import org.slf4j.LoggerFactory;

/**
 * Klasse som kan brukes som class-rule i JUnit-tester for å sjekke etter spesifikke logg-hendelser.
 * @author Snorre E. Brekke - Computas
 */
public class LogbackVerifier extends ExternalResource {

    private final List<ILoggingEvent> events = new ArrayList<>();

    @Mock
    private Appender<ILoggingEvent> appender;

    public LogbackVerifier() {
        initMocks(this);
        when(appender.getName()).thenReturn("MOCK");
        doAnswer(invocation -> {
            events.add((ILoggingEvent) invocation.getArguments()[0]);
            return null;
        }).when(appender).doAppend(any(ILoggingEvent.class));
    }

    @Override
    protected void before() {
        reset();

        final Logger rootLogger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        rootLogger.detachAndStopAllAppenders();
        rootLogger.addAppender(appender);
    }

    @Override
    protected void after() {
        reset();

        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        try {
            JoranConfigurator configurator = new JoranConfigurator();
            configurator.setContext(context);
            context.reset();
            configurator.doConfigure(LogbackVerifier.class.getClassLoader().getResourceAsStream("logback-test.xml"));
        } catch (JoranException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Verifiserer loggmeldinger på angitt nivå.
     *
     * @param level alle loggnivå som det skal skjekkes meldinger for
     * @return en asserter som inneholder en liste med den tekstlige meldingen til logg-hendelsene på angitt nivå.
     */
    @SuppressWarnings("unchecked")
    public AbstractListAssert<?, ? extends List<String>, String> assertMessagesWithLevel(final Level... level) {
        final HashSet<Level> tmp = new HashSet<>(asList(level));
        return (AbstractListAssert<?, ? extends List<String>, String>) assertThat(
                events()
                        .filter(e -> tmp.contains(e.getLevel()))
                        .map(ILoggingEvent::getFormattedMessage)
                        .collect(toList())
        ).as("messages with level " + tmp.stream().map(Level::toString).collect(joining(" or ")));
    }

    /**
     * Verifiserer exception-meldinger knyttet til angitte exception-klasser.
     *
     * @param expectedExeptions alle typene til feil som det skal skjekkes meldinger for
     * @return en asserter som inneholder en liste med den tekstlige meldingen til feilmeldingen for angitt exception.
     */
    @SuppressWarnings("unchecked")
    public final AbstractListAssert<?, ? extends List<String>, String> assertMessagesForExceptions(final Collection<Class<? extends Exception>> expectedExeptions) {
        return (AbstractListAssert<?, ? extends List<String>, String>) assertThat(
                events()
                        .map(ILoggingEvent::getThrowableProxy)
                        .filter(t -> isSubtypeOf(t, expectedExeptions))
                        .map(IThrowableProxy::getMessage)
                        .collect(toList())
        ).as("exceptions with subtype of " + expectedExeptions.stream().map(Class::getName).collect(joining(" or ")));
    }

    public final AbstractCharSequenceAssert<?, String> assertMessagesAsString(){
        return  assertThat(events()
                .map(ILoggingEvent::getFormattedMessage)
                .collect(Collectors.joining("\n")));
    }

    public Stream<ILoggingEvent> events() {
        return events.stream();
    }

    private void reset() {
        events.clear();
    }

    private boolean isSubtypeOf(IThrowableProxy throwable, final Collection<Class<? extends Exception>> expectedExeptions) {
        try {
            Class<?> type = Class.forName(throwable.getClassName());
            return expectedExeptions.stream().filter(t -> t.isAssignableFrom(type)).findAny().isPresent();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

}
