/***************************************************************************
 *   Copyright (C) 2011 by Paul Lutus                                      *
 *   lutusp@arachnoid.com                                                  *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *                                                                         *
 *   This program is distributed in the hope that it will be useful,       *
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of        *
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the         *
 *   GNU General Public License for more details.                          *
 *                                                                         *
 *   You should have received a copy of the GNU General Public License     *
 *   along with this program; if not, write to the                         *
 *   Free Software Foundation, Inc.,                                       *
 *   59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.             *
 ***************************************************************************/
package jnx;

import javax.sound.sampled.*;
import java.nio.*;
import java.util.*;
import java.util.regex.*;
import java.awt.*;

/**
 *
 * @author lutusp
 */
final public class NavtexReceiver extends Thread {

    public enum State {

        NOSIGNAL, SYNC_SETUP, SYNC1, SYNC2, READ_DATA
    }
    JNX parent;
    boolean pll_mode = false;
    SourceDataLine sourceDataLine = null;
    TargetDataLine targetDataLine = null;
    AudioFormat audioFormat = null;
    CCIR476 ccir476;
    java.util.List<Integer> sync_chars;
    StringBuffer reception_queue;
    int c1, c2, c3;
    byte[] bbuffer;
    byte[] out_buffer;
    short[] sbuffer;
    int zero_crossing_samples = 16;
    int zero_crossings_divisor = 4;
    int[] zero_crossings = null;
    long zero_crossing_count;
    int succeed_tally;
    int fail_tally;
    double invsqr2 = 1.0 / Math.sqrt(2);
    double message_time = 0;
    int bbufsz = 8192;
    int sbufsz = bbufsz / 2;
    boolean thread_enabled = false;
    boolean in_thread_inner_loop = false;
    boolean thread_exit;
    int available;
    int read_length;
    double sample_interval;
    double signal_accumulator = 0;
    double logic_level = 0;
    double mark_f, space_f;
    double audio_average = 0;
    double audio_average_tc;
    double audio_minimum = 256;
    double time_sec;
    double dv, bpdv, sign;
    double space_level, mark_level;
    double space_abs, mark_abs;
    final double default_baud_rate = 100;
    final int default_sample_rate = 48000;
    final double default_sideband_frequency = 1000;
    final int default_audio_source = 0;
    final int default_audio_dest = 0;
    double baud_rate = default_baud_rate;
    double baud_error;
    int sample_rate = default_sample_rate;
    int audio_source = default_audio_source;
    int audio_dest = default_audio_dest;
    boolean mark_state, averaged_mark_state;
    int bit_duration = 0;
    boolean old_mark_state;
    BiQuadraticFilter biquad_mark, biquad_space;
    BiQuadraticFilter biquad_lowpass;
    double bit_duration_seconds;
    long bit_duration_delta;
    int bit_sample_count, half_bit_sample_count;
    State state;
    long sample_count;
    long next_event_count = 0;
    int bit_count;
    int code_bits;
    int code_word;
    int idle_word;
    boolean shift = false;
    final int code_character32 = 0x6a;
    final int code_ltrs = 0x5a;
    final int code_figs = 0x36;
    final int code_alpha = 0x0f;
    final int code_beta = 0x33;
    final int code_char32 = 0x6a;
    final int code_rep = 0x66;
    final int char_bell = 0x07;
    final String start_valid;
    final String stop_valid;
    Pattern start_valid_regex;
    boolean inverse = false;
    boolean pulse_edge_event;
    int error_count;
    int valid_count;
    double diffabs;
    java.util.List<Mixer.Info> source_mixer_list, target_mixer_list;
    Line.Info targetLineInfo;
    Line.Info sourceLineInfo;
    double reset_target_time = 7200; // 2 hours
    int spectrum_choice = 0;
    double sync_delta;
    boolean alpha_phase = false;
    boolean valid_message = false;
    boolean navtex_message_filtering = false;
    Color navtex_valid, navtex_invalid;
    // filter method related
    double center_frequency_f = 1000.0;
    double deviation_f = 90.0;
    double lowpass_filter_f = 140.0;
    double mark_space_filter_q;
    double pll_val = 0, pll_out = 0;
    double pll_integral = 0;
    double pll_reference = 0;
    double pll_loop_control;
    double pll_loop_gain = .7;
    double pll_center_f = 1000.0;
    double pll_omega;
    double pll_bandpass_q = 2;
    double pll_bandpass_deviation_f = 90.0;
    double pll_loop_lowpass_filter_f = 1000;
    double pll_output_lowpass_filter_f = 100;
    BiQuadraticFilter biquad_pll_bandpass1, biquad_pll_bandpass2;
    BiQuadraticFilter biquad_pll_loop_lowpass;
    BiQuadraticFilter biquad_pll_output_lowpass;

    public NavtexReceiver(JNX p) {
        parent = p;
        start_valid = "ZCZC xx00\r"; // only for length
        // the pattern to match
        start_valid_regex = Pattern.compile("ZCZC \\w\\w\\d\\d\\r");
        stop_valid = "\r\nNNNN\r\n";
        state = State.NOSIGNAL;
        sync_chars = new ArrayList<Integer>();
        reception_queue = new StringBuffer();
        ccir476 = new CCIR476();
        create_mixer_lists();
        biquad_mark = new BiQuadraticFilter();
        biquad_space = new BiQuadraticFilter();
        biquad_lowpass = new BiQuadraticFilter();
        biquad_pll_loop_lowpass = new BiQuadraticFilter();
        biquad_pll_output_lowpass = new BiQuadraticFilter();
        biquad_pll_bandpass1 = new BiQuadraticFilter();
        biquad_pll_bandpass2 = new BiQuadraticFilter();
        navtex_invalid = new Color(1.0f, 1.0f, .8f);
        navtex_valid = new Color(.8f, 1.0f, .8f);
        start();
    }

    private void create_mixer_lists() {
        targetLineInfo = new Line.Info(TargetDataLine.class);
        sourceLineInfo = new Line.Info(SourceDataLine.class);
        Mixer.Info[] mi_list = AudioSystem.getMixerInfo();
        source_mixer_list = new java.util.ArrayList<Mixer.Info>();
        target_mixer_list = new java.util.ArrayList<Mixer.Info>();
        for (Mixer.Info mi : mi_list) {
            Mixer mixer = AudioSystem.getMixer(mi);
            if (mixer.isLineSupported(targetLineInfo)) {
                target_mixer_list.add(mi);
                //CommonCode.p("accept target " + mixer);
            }
            if (mixer.isLineSupported(sourceLineInfo)) {
                source_mixer_list.add(mi);
            }
        }
    }

    public void setup_values() {
        close_audio_lines();
        // changes in any of these values will cause a reset
        audio_source = parent.audio_source.get_value();
        audio_dest = parent.audio_dest.get_value();
        sample_rate = parent.sample_rate.get_value();
        parent.time_scope_pane.reset_scope_size();
        baud_rate = parent.baud_rate.get_dvalue();
        audio_average_tc = 1000.0 / sample_rate;
        // this value must never be zero
        baud_rate = (baud_rate < 10) ? 10 : baud_rate;
        bit_duration_seconds = 1.0 / baud_rate;
        bit_sample_count = (int) (sample_rate * bit_duration_seconds + 0.5);
        half_bit_sample_count = bit_sample_count / 2;
        pulse_edge_event = false;
        shift = false;
        error_count = 0;
        valid_count = 0;
        sample_interval = 1.0 / sample_rate;
        inverse = true;
        bbuffer = new byte[bbufsz];
        out_buffer = new byte[bbufsz];
        sbuffer = new short[sbufsz];
        sample_count = 0;
        next_event_count = 0;
        zero_crossing_count = 0;
        zero_crossings = new int[bit_sample_count / zero_crossings_divisor];
        sync_delta = 0;
        update_filters();
    }

    public void update_filters() {
        set_filter_values();
        configure_filters();
    }

    private void set_filter_values() {
        // carefully manage the parameters WRT the center frequency
        center_frequency_f = parent.center_frequency.get_dvalue();
        // Q must change with frequency
        mark_space_filter_q = 6 * center_frequency_f / 1000.0;
        // try to maintain a zero mixer output
        // at the carrier frequency
        double qv = center_frequency_f + (4.0 * 1000 / center_frequency_f);
        mark_f = qv + deviation_f;
        space_f = qv - deviation_f;
        pll_omega = 2 * Math.PI * center_frequency_f;
        biquad_pll_loop_lowpass.configure(BiQuadraticFilter.Type.LOWPASS, center_frequency_f, sample_rate, invsqr2);
        biquad_pll_bandpass1.configure(BiQuadraticFilter.Type.BANDPASS, center_frequency_f - pll_bandpass_deviation_f, sample_rate, pll_bandpass_q);
        biquad_pll_bandpass2.configure(BiQuadraticFilter.Type.BANDPASS, center_frequency_f + pll_bandpass_deviation_f, sample_rate, pll_bandpass_q);
    }

    private void configure_filters() {
        biquad_mark.configure(BiQuadraticFilter.Type.BANDPASS, mark_f, sample_rate, mark_space_filter_q);
        biquad_space.configure(BiQuadraticFilter.Type.BANDPASS, space_f, sample_rate, mark_space_filter_q);
        biquad_lowpass.configure(BiQuadraticFilter.Type.LOWPASS, lowpass_filter_f, sample_rate, invsqr2);

        biquad_pll_loop_lowpass = new BiQuadraticFilter(BiQuadraticFilter.Type.LOWPASS, pll_loop_lowpass_filter_f, sample_rate, invsqr2);
        biquad_pll_output_lowpass = new BiQuadraticFilter(BiQuadraticFilter.Type.LOWPASS, pll_output_lowpass_filter_f, sample_rate, invsqr2);
        biquad_pll_bandpass1 = new BiQuadraticFilter(BiQuadraticFilter.Type.BANDPASS, pll_center_f - pll_bandpass_deviation_f, sample_rate, pll_bandpass_q);
        biquad_pll_bandpass2 = new BiQuadraticFilter(BiQuadraticFilter.Type.BANDPASS, pll_center_f + pll_bandpass_deviation_f, sample_rate, pll_bandpass_q);

    }

    public void periodic_actions() {
        spectrum_choice = parent.spectrum_selection.get_index();
        test_audio_restart();
        control_monitor_line(thread_enabled && parent.monitor_volume.get_dvalue() > 0);
        navtex_message_filtering = parent.navtex_filter.get_value();
        parent.message_filter_button.setEnabled(navtex_message_filtering);
        if (!navtex_message_filtering) {
            this.valid_message = false;
        }
        Color col = (valid_message || !navtex_message_filtering) ? navtex_valid : navtex_invalid;
        parent.navtex_filter_checkbox.setBackground(col);
        //parent.p(audio_average);
    }

    public void test_audio_restart() {
        // this brutal remedy seems necessary
        // to deal with a bad audio system bug
        // that freezes a program at exactly 163484 seconds
        if (time_sec > reset_target_time && state != state.READ_DATA) {
            restart();
        }
    }

    public void restart() {
        control(false);
        control(true);
    }

    public void control(boolean run) {
        if (run) {
            if (!thread_enabled) {
                setup_values();
                thread_enabled = true;
            }
        } else {
            if (thread_enabled) {
                thread_enabled = false;
                while (in_thread_inner_loop) {
                    try {
                        Thread.sleep(10);
                    } catch (Exception e) {
                        parent.trace_errors("", e);
                    }
                }
            }
        }
    }

    public void end_thread() {
        try {
            thread_enabled = false;
            thread_exit = true;
            join();
        } catch (Exception e) {
            parent.trace_errors("", e);
        }

    }

    public boolean enabled() {
        return thread_enabled;
    }

    @Override
    public void run() {
        try {
            thread_exit = false;
            while (!thread_exit) {
                // You might think this double-enclosure
                // isn't necessary, but you would be wrong
                while (thread_enabled) {
                    in_thread_inner_loop = true;
                    if (open_target_line(true, true)) {
                        while (thread_enabled && targetDataLine != null) {
                            while (thread_enabled && (available = targetDataLine.available()) < bbufsz && available >= 0) {
                                Thread.sleep(20);
                            }
                            // process the acquired audio data
                            if (thread_enabled) {
                                read_length = targetDataLine.read(bbuffer, 0, bbufsz);
                                if (read_length > 0) {
                                    ShortBuffer sb = ByteBuffer.wrap(bbuffer).asShortBuffer();
                                    sb.get(sbuffer);
                                    process_data(sbuffer);
                                    write_output(sbuffer);
                                }
                            }
                        }
                    }
                    close_audio_lines();
                    in_thread_inner_loop = false;
                }
                // wait for restart
                if (!thread_exit) {
                    Thread.sleep(100);
                }
            }
        } catch (Exception e) {
            parent.trace_errors(getClass().getSimpleName() + ".run: ", e);
        }
    }

    private void set_state(State s) {
        if (s != state) {
            state = s;
            //parent.p("New state: " + state);
        }
    }

    private String char_queue_compare_regex() {
        String result = null;
        int qlen = reception_queue.length();
        int slen = start_valid.length();
        if (qlen < slen) {
            return null;
        }
        String comp = reception_queue.subSequence(qlen - slen, qlen).toString();
        Matcher m = start_valid_regex.matcher(comp);
        if (m.matches()) {
            String s = "" + comp.charAt(6);
            //parent.p("Matching char " + s);
            if (parent.accepted_navtex_messages.accept(s)) {
                result = comp;
            }
        }
        return result;
    }

    private boolean char_queue_compare(String s) {
        int qlen = reception_queue.length();
        int slen = s.length();
        if (qlen < slen) {
            return false;
        }
        String comp = reception_queue.subSequence(qlen - slen, qlen).toString();
        boolean r = comp.equals(s);
        //parent.debug_p(reception_queue.length() + " compared [" + s + "] to [" + comp + "] = " + r + ", valid message = " + valid_message);
        return r;
    }

    private void char_queue(int c) {
        if (navtex_message_filtering) {
            String s;
            reception_queue.append((char) c);
            while (reception_queue.length() > 16) {
                reception_queue.deleteCharAt(0);
            }
            if (!valid_message) {
                if ((s = char_queue_compare_regex()) != null) {
                    valid_message = true;
                    message_time = time_sec;
                    for (int i = 0; i < s.length(); i++) {
                        filter_print(s.charAt(i));
                    }
                }
            } else { // valid message state
                // if stop mark detected or 10-min time limit exceeded
                if (char_queue_compare(stop_valid) || (time_sec - message_time > 600)) {
                    valid_message = false;
                }
            }
        }
    }

    private void debug_print(int code) {
        int ch = ccir476.code_to_char(code, false);
        ch = (ch < 0) ? '_' : ch;
        /*if (code == code_rep) {
        alpha_channel = true;
        }
        if (code == code_alpha) {
        alpha_channel = true;
        }*/
        String qs = alpha_phase ? "alpha                                " : "rep";
        //System.out.println(String.format("%s|%s: count = %d, code = %x, ch = %c, shift = %s",qs, s, char_count, code, ch, shift ? "True" : "False"));
        System.out.println(String.format("%s|%x:%c", qs, code, ch));
        //alpha_channel = !alpha_channel;
    }

    // two phases: alpha and rep
    // marked during sync by code_alpha and code_rep
    // then for data: rep phase character is sent first,
    // then, three chars later, same char is sent in alpha phase
    private boolean process_char(int code) {
        //debug_print(code);
        boolean success = CCIR476.check_bits(code);
        int chr = -1;
        // force phasing with the two phasing characters
        if (code == code_rep) {
            alpha_phase = false;
        } else if (code == code_alpha) {
            alpha_phase = true;
        }
        if (!alpha_phase) {
            c1 = c2;
            c2 = c3;
            c3 = code;
        } else { // alpha channel
            if (parent.strict.get_value()) {
                if (success && c1 == code) {
                    chr = code;
                }
            } else {
                if (success) {
                    chr = code;
                } else if (CCIR476.check_bits(c1)) {
                    chr = c1;
                    //parent.debug_p(String.format("FEC replacement: %x -> %x", code, c1));
                }
            }
            if (chr == -1) {
                fail_tally++;
                //parent.debug_p(String.format("fail all options: %x %x", code, c1)); 
            } else {
                succeed_tally++;

                switch (chr) {
                    case code_rep:
                        break;
                    case code_alpha:
                        break;
                    case code_beta:
                        break;
                    case code_char32:
                        break;
                    case code_ltrs:
                        shift = false;
                        break;
                    case code_figs:
                        shift = true;
                        break;
                    default:
                        chr = ccir476.code_to_char(chr, shift);
                        if (chr < 0) {
                            parent.debug_p(String.format("missed this code: %x", Math.abs(chr)));
                        } else {
                            if (!navtex_message_filtering || valid_message) {
                                filter_print(chr);
                            }
                            char_queue(chr);
                        }
                        break;
                } // switch

            } // if test != -1
            //parent.debug_p(String.format("compare: %x = %x, %s", code, c1, (code == c1) ? "YES" : "NO"));
        } // alpha channel

        // alpha/rep phasing
        alpha_phase = !alpha_phase;
        return success;
    }

    private void filter_print(int c) {
        if (c == char_bell) {
            parent.beep();
        } else if (c != -1 && c != '\r' && c != code_alpha && c != code_rep) {
            parent.append_to_data_page("" + (char) c);
        }
    }

    private double max(double a, double b) {
        return (a > b) ? a : b;
    }

    private void process_data(short[] data) {
        for (short v : data) {
            time_sec = sample_count * sample_interval;
            dv = v;

            if (pll_mode) {

                // PLL block

                audio_average += (Math.abs(dv) - audio_average) * audio_average_tc;

                audio_average = Math.max(.1, audio_average);
                dv /= audio_average;

                dv = biquad_pll_bandpass1.filter(dv);
                dv = biquad_pll_bandpass2.filter(dv);

                pll_loop_control = dv * pll_reference * pll_loop_gain;
                pll_loop_control = biquad_pll_loop_lowpass.filter(pll_loop_control);
                pll_integral += pll_loop_control * sample_interval;
                if (Double.isInfinite(pll_integral)) {
                    pll_integral = 0;
                }
                pll_reference = Math.sin(pll_omega * (time_sec + pll_integral));
                logic_level = biquad_pll_output_lowpass.filter(pll_loop_control);
                logic_level *= center_frequency_f / 85.0;

            } else {

                // separate mark and space by narrow filtering
                mark_level = biquad_mark.filter(dv);
                space_level = biquad_space.filter(dv);

                mark_abs = Math.abs(mark_level);
                space_abs = Math.abs(space_level);

                audio_average += (max(mark_abs, space_abs) - audio_average) * audio_average_tc;

                audio_average = Math.max(.1, audio_average);

                // produce difference of absolutes of mark and space
                diffabs = (mark_abs - space_abs);

                diffabs /= audio_average;

                dv /= audio_average;

                mark_level /= audio_average;
                space_level /= audio_average;

                // now low-pass the resulting difference
                logic_level = biquad_lowpass.filter(diffabs);

            }

            mark_state = (logic_level > 0);
            signal_accumulator += (mark_state) ? 1 : -1;
            bit_duration++;

            // adjust signal synchronization over time
            // by detecting zero crossings

            if (zero_crossings != null) {
                if (mark_state != old_mark_state) {
                    // a valid bit duration must be longer than bit duration / 2
                    if ((bit_duration % bit_sample_count) > half_bit_sample_count) {
                        // create a relative index for this zero crossing
                        int index = (int) ((sample_count - next_event_count + bit_sample_count * 8) % bit_sample_count);
                        zero_crossings[index / zero_crossings_divisor]++;
                    }
                    bit_duration = 0;
                }
                old_mark_state = mark_state;
                if (sample_count % bit_sample_count == 0) {
                    zero_crossing_count++;
                    if (zero_crossing_count >= zero_crossing_samples) {
                        int best = 0;
                        double index = 0;
                        int q;
                        // locate max zero crossing
                        for (int i = 0; i < zero_crossings.length; i++) {
                            q = zero_crossings[i];
                            zero_crossings[i] = 0;
                            if (q > best) {
                                best = q;
                                index = i;
                            }
                        }
                        if (best > 0) { // if there is a basis for choosing
                            // create a signed correction value
                            index *= zero_crossings_divisor;
                            index = ((index + half_bit_sample_count) % bit_sample_count) - half_bit_sample_count;
                            //parent.p("error index: " + index);
                            // limit loop gain
                            index /= 8.0;
                            // sync_delta is a temporary value that is
                            // used once, then reset to zero
                            sync_delta = index;
                            // baud_error is persistent -- used by baud error label
                            baud_error = index;
                        }
                        zero_crossing_count = 0;
                    }
                }
            }

            // flag the center of signal pulses
            pulse_edge_event = sample_count >= next_event_count;
            if (pulse_edge_event) {
                averaged_mark_state = (signal_accumulator > 0) ^ inverse;
                signal_accumulator = 0;
                // set new timeout value, include zero crossing correction
                next_event_count = sample_count + bit_sample_count + (int) (sync_delta + 0.5);
                sync_delta = 0;
            }

            // manage the scope displays
            if (parent.scope_visible) {
                parent.time_scope_pane.write_value(logic_level);
                double sv = 0;
                switch (spectrum_choice) {
                    case 2:
                        sv = dv;
                        break;
                    case 1:
                        sv = space_level;
                        break;
                    case 0:
                        sv = mark_level;
                        break;
                }
                //parent.spectrum_manager.add_data(sv / audio_average);
                parent.spectrum_manager.add_data(sv);
            }

            if (audio_average < audio_minimum) {
                set_state(State.NOSIGNAL);
            } else if (state == State.NOSIGNAL) {
                state = State.SYNC_SETUP;
            }

            switch (state) {
                case SYNC_SETUP:
                    bit_count = -1;
                    code_bits = 0;
                    error_count = 0;
                    valid_count = 0;
                    shift = false;
                    sync_chars.clear();
                    state = State.SYNC1;
                    break;
                // scan indefinitely for valid bit pattern
                case SYNC1:
                    if (pulse_edge_event) {
                        code_bits = (code_bits >> 1) | ((averaged_mark_state) ? 64 : 0);
                        if (CCIR476.check_bits(code_bits)) {
                            //debug_print("first valid");
                            sync_chars.add(code_bits);
                            bit_count = 0;
                            code_bits = 0;
                            state = State.SYNC2;
                        }
                    }
                    break;
                //  sample and validate bits in groups of 7
                case SYNC2:
                    // find any bit alignment that produces a valid character
                    // then test that synchronization in subsequent groups of 7 bits
                    if (pulse_edge_event) {
                        code_bits = (code_bits >> 1) | ((averaged_mark_state) ? 64 : 0);
                        bit_count++;
                        if (bit_count == 7) {
                            if (CCIR476.check_bits(code_bits)) {
                                //debug_print("next valid char");
                                sync_chars.add(code_bits);
                                code_bits = 0;
                                bit_count = 0;
                                valid_count++;
                                // successfully read 4 characters?
                                if (valid_count == 4) {
                                    for (Integer c : sync_chars) {
                                        process_char(c);
                                    }
                                    state = State.READ_DATA;
                                }
                            } else { // failed subsequent bit test
                                code_bits = 0;
                                bit_count = 0;
                                //debug_print("restarting sync");
                                state = State.SYNC_SETUP;
                            }
                        }
                    }
                    break;
                case READ_DATA:
                    if (pulse_edge_event) {
                        code_bits = (code_bits >> 1) | ((averaged_mark_state) ? 64 : 0);
                        bit_count++;
                        if (bit_count == 7) {
                            if (error_count > 0) {
                                //parent.debug_p("error count: " + error_count);
                            }
                            if (process_char(code_bits)) {
                                if (error_count > 0) {
                                    error_count--;
                                }
                            } else {
                                error_count++;
                                if (error_count > 2) {
                                    //parent.debug_p("returning to sync");
                                    state = State.SYNC_SETUP;
                                }
                            }
                            bit_count = 0;
                            code_bits = 0;
                        }
                    }
                    break;
            }

            sample_count++;
        }
    }

    private void close_audio_lines() {
        if (targetDataLine != null) {
            targetDataLine.stop();
            targetDataLine.close();
            targetDataLine = null;
        }
        if (sourceDataLine != null) {
            sourceDataLine.stop();
            sourceDataLine.close();
            sourceDataLine = null;
        }
    }

    public boolean open_target_line(boolean run, boolean force) {
        try {
            if (run && target_mixer_list.size() > 0) {
                set_state(State.SYNC_SETUP);
                shift = false;
                if (targetDataLine != null || force) {
                    close_audio_lines();
                    Mixer.Info mi;
                    int n = audio_source;
                    n = Math.min(n, target_mixer_list.size() - 1);
                    mi = target_mixer_list.get(n);
                    Mixer mixer = AudioSystem.getMixer(mi);
                    audioFormat = define_audio_format();
                    DataLine.Info info = new DataLine.Info(TargetDataLine.class, audioFormat);
                    targetDataLine = (TargetDataLine) mixer.getLine(targetLineInfo);
                    // provide the desired buffer size
                    targetDataLine.open(audioFormat, bbufsz * 2);
                    targetDataLine.start();
                    thread_enabled = run;
                }
            } else {
                close_audio_lines();
            }
        } catch (Exception e) {
            targetDataLine = null;
            parent.trace_errors(getClass().getSimpleName() + ".init: ", e);
            return false;
        }
        return true;

    }

    private void control_monitor_line(boolean run) {
        try {
            if (run && source_mixer_list.size() > 0) {
                if (sourceDataLine == null && source_mixer_list.size() > 0) {
                    int i = parent.audio_dest.get_value();
                    i = (i >= source_mixer_list.size()) ? source_mixer_list.size() - 1 : i;
                    audioFormat = define_audio_format();
                    Mixer.Info mi = source_mixer_list.get(i);
                    Mixer mixer = AudioSystem.getMixer(mi);
                    sourceDataLine = (SourceDataLine) mixer.getLine(sourceLineInfo);
                    sourceDataLine.open(audioFormat);
                    sourceDataLine.start();
                }
            } else {
                if (sourceDataLine != null) {
                    sourceDataLine.stop();
                    sourceDataLine.close();
                    sourceDataLine = null;
                }
            }
        } catch (Exception e) {
            sourceDataLine = null;
            parent.trace_errors("control_monitor_line: ", e);
        }
    }

    public void write_output(short[] data) {
        if (sourceDataLine != null) {
            double gain = parent.monitor_volume.get_pct_dvalue();
            ByteBuffer byb = ByteBuffer.wrap(out_buffer);
            // must loop through to spply gain setting
            for (short sv : data) {
                byb.putShort((short) (sv * gain));
            }
            sourceDataLine.write(out_buffer, 0, out_buffer.length);
        }
    }

    AudioFormat define_audio_format() {
        int sampleSizeInBits = 16;
        int channels = 1;
        boolean signed = true;
        boolean bigEndian = true;
        float rate = sample_rate;
        return new AudioFormat(
                rate,
                sampleSizeInBits,
                channels,
                signed,
                bigEndian);
    }
}
