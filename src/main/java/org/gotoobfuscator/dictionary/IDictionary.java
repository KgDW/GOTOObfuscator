package org.gotoobfuscator.dictionary;

import org.gotoobfuscator.Obfuscator;
import org.gotoobfuscator.dictionary.impl.AlphaDictionary;
import org.gotoobfuscator.dictionary.impl.CustomDictionary;
import org.gotoobfuscator.dictionary.impl.NumberDictionary;
import org.gotoobfuscator.dictionary.impl.UnicodeDictionary;

public interface IDictionary {
    String get();

    static IDictionary newDictionary() {
        switch (Obfuscator.Instance.getDictionaryMode()) {
            case 0:
            default:
                return new AlphaDictionary();
            case 1:
                return new NumberDictionary();
            case 2:
                return new UnicodeDictionary(1);
            case 3:
                return new CustomDictionary();
        }
    }
}
