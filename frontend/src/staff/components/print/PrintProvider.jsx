import { createContext, useCallback, useContext, useEffect, useRef, useState } from 'react';
import { createPortal } from 'react-dom';
import { Icon } from '../../../shared/ui/Icon.jsx';
import { AppointmentTicketPrint } from './AppointmentTicketPrint.jsx';
import { RecordPrintSheet } from './RecordPrintSheet.jsx';
import { PrescriptionPrintSheet } from './PrescriptionPrintSheet.jsx';
import './print.css';

const PrintContext = createContext({
  printTicket: () => {},
  printRecord: () => {},
  printPrescription: () => {},
  isPrinting: false,
});

/**
 * Hook sử dụng tính năng in tại quầy.
 */
export function usePrint() {
  return useContext(PrintContext);
}

/**
 * Provider quản lý tiến trình in ấn dùng chung cho toàn bộ khu nội bộ.
 * Đảm bảo:
 * - Chống bấm in 2 lần liên tiếp (khóa isPrinting).
 * - Thông báo nhỏ toast "Đã mở hộp thoại in" không chặn màn hình.
 * - Render ra portal #staff-print-portal.
 */
export function PrintProvider({ children }) {
  const [activePrint, setActivePrint] = useState(null);
  const [toast, setToast] = useState(false);
  const [isPrinting, setIsPrinting] = useState(false);
  const lockRef = useRef(false);
  const toastTimerRef = useRef(null);

  // Dọn dẹp toast timer khi unmount
  useEffect(() => {
    return () => {
      if (toastTimerRef.current) clearTimeout(toastTimerRef.current);
    };
  }, []);

  // Lắng nghe sự kiện afterprint của trình duyệt để gỡ khoá an toàn
  useEffect(() => {
    const handleAfterPrint = () => {
      lockRef.current = false;
      setIsPrinting(false);
      // Giữ activePrint một chút để trình duyệt hoàn tất spooling
      setTimeout(() => {
        setActivePrint(null);
      }, 500);
    };

    window.addEventListener('afterprint', handleAfterPrint);
    return () => window.removeEventListener('afterprint', handleAfterPrint);
  }, []);

  const triggerPrint = useCallback((target) => {
    // Chống bấm 2 lần liên tiếp
    if (lockRef.current) return;
    lockRef.current = true;
    setIsPrinting(true);
    setActivePrint(target);

    // Hiển thị thông báo nhỏ "Đã mở hộp thoại in"
    setToast(true);
    if (toastTimerRef.current) clearTimeout(toastTimerRef.current);
    toastTimerRef.current = setTimeout(() => {
      setToast(false);
    }, 3500);

    // Chờ React cập nhật DOM vào portal trước khi gọi hộp thoại in của trình duyệt
    setTimeout(() => {
      try {
        window.print();
      } catch (e) {
        console.error('Không thể kích hoạt hộp thoại in:', e);
      }
      // Fallback gỡ khoá sau 1.5 giây nếu sau print không nhận afterprint
      setTimeout(() => {
        lockRef.current = false;
        setIsPrinting(false);
      }, 1500);
    }, 120);
  }, []);

  const printTicket = useCallback((appt, variant = 'a5') => {
    if (!appt) return;
    triggerPrint({ type: 'ticket', appt, variant });
  }, [triggerPrint]);

  const printRecord = useCallback((record, patient, variant = 'a5') => {
    if (!record) return;
    triggerPrint({ type: 'record', record, patient, variant });
  }, [triggerPrint]);

  const printPrescription = useCallback((record, patient, variant = 'a5') => {
    if (!record) return;
    triggerPrint({ type: 'prescription', record, patient, variant });
  }, [triggerPrint]);

  return (
    <PrintContext.Provider value={{ printTicket, printRecord, printPrescription, isPrinting }}>
      {children}

      {/* Thông báo nhỏ (Toast) Elderly-First không popup chặn */}
      {toast && (
        <div className="print-toast" role="status" aria-live="polite">
          <Icon name="checkCircle" size={24} strokeWidth={2.25} />
          <span>Đã mở hộp thoại in</span>
        </div>
      )}

      {/* Vùng in Portal gắn vào body, chỉ hiển thị khi in (@media print) */}
      {activePrint &&
        createPortal(
          <div id="staff-print-portal" aria-hidden="true">
            {activePrint.type === 'ticket' && (
              <AppointmentTicketPrint appt={activePrint.appt} variant={activePrint.variant} />
            )}
            {activePrint.type === 'record' && (
              <RecordPrintSheet
                record={activePrint.record}
                patient={activePrint.patient}
                variant={activePrint.variant}
              />
            )}
            {activePrint.type === 'prescription' && (
              <PrescriptionPrintSheet
                record={activePrint.record}
                patient={activePrint.patient}
                variant={activePrint.variant}
              />
            )}
          </div>,
          document.body,
        )}
    </PrintContext.Provider>
  );
}
