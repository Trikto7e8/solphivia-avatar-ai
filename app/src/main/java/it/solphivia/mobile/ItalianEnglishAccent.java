package it.solphivia.mobile;

import java.text.Normalizer;
import java.util.Locale;

/** Lightweight respelling for reading Italian words with an English-language TTS voice. */
final class ItalianEnglishAccent {
    private ItalianEnglishAccent() { }

    static String respell(String text) {
        if (text == null || text.isEmpty()) return text;
        String value = Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
        StringBuilder result = new StringBuilder(value.length() + 16);
        for (String word : value.split("(?<=\\s)|(?=\\s)")) {
            if (word.trim().isEmpty() || !word.matches(".*[\\p{L}].*")) {
                result.append(word);
                continue;
            }
            String leading = word.replaceFirst("^([^\\p{L}]*).*", "$1");
            String trailing = word.replaceFirst(".*?([\\p{L}]+)([^\\p{L}]*)$", "$2");
            String core = word.substring(leading.length(), word.length() - trailing.length());
            String converted = convertWord(core.toLowerCase(Locale.ROOT));
            if (!core.isEmpty() && Character.isUpperCase(core.charAt(0))) {
                converted = Character.toUpperCase(converted.charAt(0)) + converted.substring(1);
            }
            result.append(leading).append(converted).append(trailing);
        }
        return result.toString();
    }

    private static String convertWord(String word) {
        // Common words benefit from an explicit hint; the remaining rules handle Italian spelling.
        switch (word) {
            case "ciao": return "chow";
            case "io": return "eeoh";
            case "e": return "eh";
            case "di": return "dee";
            case "ti": return "tee";
            case "mi": return "mee";
            case "si": return "see";
            case "vi": return "vee";
            case "qui": return "kwee";
            case "chi": return "kee";
            case "che": return "keh";
            case "per": return "pehr";
            case "grazie": return "grahtsyeh";
            case "arrivederci": return "ahreevehdehrchee";
            case "perche": return "pehrkeh";
            case "come": return "kohmeh";
            case "sono": return "sohnoh";
            case "posso": return "pohssoh";
            case "aiuto": return "ahyootoe";
            case "aiutare": return "ahyootehreh";
            case "aiutarti": return "ahyootehrtee";
            case "buongiorno": return "bwohnjornoh";
            case "buonasera": return "bwohnasehrah";
            case "buonanotte": return "bwohnanohtteh";
            case "solphivia": return "solfeeveea";
            default: break;
        }
        return word
                .replace("gli", "lyee")
                .replace("gn", "ny")
                .replace("sch", "sk")
                .replace("scia", "sha")
                .replace("scio", "sho")
                .replace("sciu", "shu")
                .replace("scie", "she")
                .replaceAll("sci(?![aeou])", "shee")
                .replaceAll("sc(?=[ei])", "sh")
                .replace("ch", "k")
                .replace("gh", "g")
                .replace("cia", "cha")
                .replace("cio", "cho")
                .replace("ciu", "chu")
                .replace("cie", "che")
                .replace("ci", "chee")
                .replace("gia", "ja")
                .replace("gio", "jo")
                .replace("giu", "ju")
                .replace("gie", "je")
                .replace("gi", "jee")
                .replaceAll("c(?=e)", "ch")
                .replaceAll("g(?=[ei])", "j")
                .replace("qu", "kw");
    }
}
