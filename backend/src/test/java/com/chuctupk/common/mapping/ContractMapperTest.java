package com.chuctupk.common.mapping;

import com.chuctupk.exception.AppException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ContractMapperTest {

    @Test
    @DisplayName("B1-1: toDbServiceType null hoặc rỗng -> mặc định KHAM_BENH")
    void testToDbServiceType_DefaultKhamBenh() {
        assertEquals("KHAM_BENH", ContractMapper.toDbServiceType(null));
        assertEquals("KHAM_BENH", ContractMapper.toDbServiceType(""));
        assertEquals("KHAM_BENH", ContractMapper.toDbServiceType("   "));
    }

    @Test
    @DisplayName("B1-2: toDbServiceType whitelist các dịch vụ hợp lệ")
    void testToDbServiceType_Whitelist() {
        assertEquals("KHAM_BENH", ContractMapper.toDbServiceType("KHAM_BENH"));
        assertEquals("CHAM_CUU", ContractMapper.toDbServiceType("cham_cuu"));
        assertEquals("XOA_BOP_BAM_HUYET", ContractMapper.toDbServiceType("XOA_BOP_BAM_HUYET"));
        assertEquals("BOC_THUOC", ContractMapper.toDbServiceType("boc_thuoc"));
        assertEquals("VAT_LY_TRI_LIEU", ContractMapper.toDbServiceType("vat_ly_tri_lieu"));
        assertEquals("CHUA_RO", ContractMapper.toDbServiceType("chua_ro"));
    }

    @Test
    @DisplayName("B1-3: toDbServiceType giá trị không hợp lệ -> ném AppException 400 Nội dung khám không hợp lệ")
    void testToDbServiceType_InvalidValue_Throws400() {
        AppException ex = assertThrows(AppException.class, () ->
                ContractMapper.toDbServiceType("PHAU_THUAT_THAM_MY")
        );
        assertEquals(400, ex.getStatusCode());
        assertEquals("Nội dung khám không hợp lệ", ex.getMessage());
    }
}
