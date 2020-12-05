// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.jna.mac;

/**
 * macOS String encoding values.
 */
enum StringEncoding {

    // This set is CFStringBuiltInEncodings 
    kCFStringEncodingMacRoman(0),
    kCFStringEncodingWindowsLatin1(0x0500), /* ANSI codepage 1252 */
    kCFStringEncodingISOLatin1(0x0201), /* ISO 8859-1 */
    kCFStringEncodingNextStepLatin(0x0B01), /* NextStep encoding*/
    kCFStringEncodingASCII(0x0600), /* 0..127 (in creating CFString, values greater than 0x7F are treated as corresponding Unicode value) */
    kCFStringEncodingUnicode(0x0100), /* kTextEncodingUnicodeDefault  + kTextEncodingDefaultFormat (aka kUnicode16BitFormat) */
    kCFStringEncodingUTF8(0x08000100), /* kTextEncodingUnicodeDefault + kUnicodeUTF8Format */
    kCFStringEncodingNonLossyASCII(0x0BFF), /* 7bit Unicode variants used by Cocoa & Java */
    kCFStringEncodingUTF16(0x0100), /* kTextEncodingUnicodeDefault + kUnicodeUTF16Format (alias of kCFStringEncodingUnicode) */
    kCFStringEncodingUTF16BE(0x10000100), /* kTextEncodingUnicodeDefault + kUnicodeUTF16BEFormat */
    kCFStringEncodingUTF16LE(0x14000100), /* kTextEncodingUnicodeDefault + kUnicodeUTF16LEFormat */
    kCFStringEncodingUTF32(0x0c000100), /* kTextEncodingUnicodeDefault + kUnicodeUTF32Format */
    kCFStringEncodingUTF32BE(0x18000100), /* kTextEncodingUnicodeDefault + kUnicodeUTF32BEFormat */
    kCFStringEncodingUTF32LE(0x1c000100), /* kTextEncodingUnicodeDefault + kUnicodeUTF32LEFormat */

    // Others are from CFStringEncodingExt.h
    kCFStringEncodingMacJapanese(1),
    kCFStringEncodingMacChineseTrad(2),
    kCFStringEncodingMacKorean(3),
    kCFStringEncodingMacArabic(4),
    kCFStringEncodingMacHebrew(5),
    kCFStringEncodingMacGreek(6),
    kCFStringEncodingMacCyrillic(7),
    kCFStringEncodingMacDevanagari(9),
    kCFStringEncodingMacGurmukhi(10),
    kCFStringEncodingMacGujarati(11),
    kCFStringEncodingMacOriya(12),
    kCFStringEncodingMacBengali(13),
    kCFStringEncodingMacTamil(14),
    kCFStringEncodingMacTelugu(15),
    kCFStringEncodingMacKannada(16),
    kCFStringEncodingMacMalayalam(17),
    kCFStringEncodingMacSinhalese(18),
    kCFStringEncodingMacBurmese(19),
    kCFStringEncodingMacKhmer(20),
    kCFStringEncodingMacThai(21),
    kCFStringEncodingMacLaotian(22),
    kCFStringEncodingMacGeorgian(23),
    kCFStringEncodingMacArmenian(24),
    kCFStringEncodingMacChineseSimp(25),
    kCFStringEncodingMacTibetan(26),
    kCFStringEncodingMacMongolian(27),
    kCFStringEncodingMacEthiopic(28),
    kCFStringEncodingMacCentralEurRoman(29),
    kCFStringEncodingMacVietnamese(30),
    kCFStringEncodingMacExtArabic(31),
    kCFStringEncodingMacSymbol(33),
    kCFStringEncodingMacDingbats(34),
    kCFStringEncodingMacTurkish(35),
    kCFStringEncodingMacCroatian(36),
    kCFStringEncodingMacIcelandic(37),
    kCFStringEncodingMacRomanian(38),
    kCFStringEncodingMacCeltic(39),
    kCFStringEncodingMacGaelic(40),
    kCFStringEncodingMacFarsi(0x8C),
    kCFStringEncodingMacUkrainian(0x98),
    kCFStringEncodingMacInuit(0xEC),
    kCFStringEncodingMacVT100(0xFC),
    kCFStringEncodingMacHFS(0xFF),
    kCFStringEncodingISOLatin2(0x0202),
    kCFStringEncodingISOLatin3(0x0203),
    kCFStringEncodingISOLatin4(0x0204),
    kCFStringEncodingISOLatinCyrillic(0x0205),
    kCFStringEncodingISOLatinArabic(0x0206),
    kCFStringEncodingISOLatinGreek(0x0207),
    kCFStringEncodingISOLatinHebrew(0x0208),
    kCFStringEncodingISOLatin5(0x0209),
    kCFStringEncodingISOLatin6(0x020A),
    kCFStringEncodingISOLatinThai(0x020B),
    kCFStringEncodingISOLatin7(0x020D),
    kCFStringEncodingISOLatin8(0x020E),
    kCFStringEncodingISOLatin9(0x020F),
    kCFStringEncodingISOLatin10(0x0210),
    kCFStringEncodingDOSLatinUS(0x0400),
    kCFStringEncodingDOSGreek(0x0405),
    kCFStringEncodingDOSBalticRim(0x0406),
    kCFStringEncodingDOSLatin1(0x0410),
    kCFStringEncodingDOSGreek1(0x0411),
    kCFStringEncodingDOSLatin2(0x0412),
    kCFStringEncodingDOSCyrillic(0x0413),
    kCFStringEncodingDOSTurkish(0x0414),
    kCFStringEncodingDOSPortuguese(0x0415),
    kCFStringEncodingDOSIcelandic(0x0416),
    kCFStringEncodingDOSHebrew(0x0417),
    kCFStringEncodingDOSCanadianFrench(0x0418),
    kCFStringEncodingDOSArabic(0x0419),
    kCFStringEncodingDOSNordic(0x041A),
    kCFStringEncodingDOSRussian(0x041B),
    kCFStringEncodingDOSGreek2(0x041C),
    kCFStringEncodingDOSThai(0x041D),
    kCFStringEncodingDOSJapanese(0x0420),
    kCFStringEncodingDOSChineseSimplif(0x0421),
    kCFStringEncodingDOSKorean(0x0422),
    kCFStringEncodingDOSChineseTrad(0x0423),
    kCFStringEncodingWindowsLatin2(0x0501),
    kCFStringEncodingWindowsCyrillic(0x0502),
    kCFStringEncodingWindowsGreek(0x0503),
    kCFStringEncodingWindowsLatin5(0x0504),
    kCFStringEncodingWindowsHebrew(0x0505),
    kCFStringEncodingWindowsArabic(0x0506),
    kCFStringEncodingWindowsBalticRim(0x0507),
    kCFStringEncodingWindowsVietnamese(0x0508),
    kCFStringEncodingWindowsKoreanJohab(0x0510),
    kCFStringEncodingANSEL(0x0601),
    kCFStringEncodingJIS_X0201_76(0x0620),
    kCFStringEncodingJIS_X0208_83(0x0621),
    kCFStringEncodingJIS_X0208_90(0x0622),
    kCFStringEncodingJIS_X0212_90(0x0623),
    kCFStringEncodingJIS_C6226_78(0x0624),
    kCFStringEncodingShiftJIS_X0213_00(0x0628),
    kCFStringEncodingShiftJIS_X0213_MenKuTen(0x0629),
    kCFStringEncodingGB_2312_80(0x0630),
    kCFStringEncodingGBK_95(0x0631),
    kCFStringEncodingGB_18030_2000(0x0632),
    kCFStringEncodingKSC_5601_87(0x0640),
    kCFStringEncodingKSC_5601_92_Johab(0x0641),
    kCFStringEncodingCNS_11643_92_P1(0x0651),
    kCFStringEncodingCNS_11643_92_P2(0x0652),
    kCFStringEncodingCNS_11643_92_P3(0x0653),
    kCFStringEncodingISO_2022_JP(0x0820),
    kCFStringEncodingISO_2022_JP_2(0x0821),
    kCFStringEncodingISO_2022_JP_1(0x0822),
    kCFStringEncodingISO_2022_JP_3(0x0823),
    kCFStringEncodingISO_2022_CN(0x0830),
    kCFStringEncodingISO_2022_CN_EXT(0x0831),
    kCFStringEncodingISO_2022_KR(0x0840),
    kCFStringEncodingEUC_JP(0x0920),
    kCFStringEncodingEUC_CN(0x0930),
    kCFStringEncodingEUC_TW(0x0931),
    kCFStringEncodingEUC_KR(0x0940),
    kCFStringEncodingShiftJIS(0x0A01),
    kCFStringEncodingKOI8_R(0x0A02),
    kCFStringEncodingBig5(0x0A03),
    kCFStringEncodingMacRomanLatin1(0x0A04),
    kCFStringEncodingHZ_GB_2312(0x0A05),
    kCFStringEncodingBig5_HKSCS_1999(0x0A06),
    kCFStringEncodingVISCII(0x0A07),
    kCFStringEncodingKOI8_U(0x0A08),
    kCFStringEncodingBig5_E(0x0A09),
    kCFStringEncodingNextStepJapanese(0x0B02),
    kCFStringEncodingEBCDIC_US(0x0C01),
    kCFStringEncodingEBCDIC_CP037(0x0C0);

    public final int value;

    StringEncoding(int value) {
        this.value = value;
    }
}
