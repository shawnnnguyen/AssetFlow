import { useState } from 'react';

export interface DashboardModals {
  showTxModal: boolean;
  showCashModal: boolean;
  showTickerModal: boolean;
  showPfModal: boolean;
  showAlertModal: boolean;
  openTxModal: () => void;
  closeTxModal: () => void;
  openCashModal: () => void;
  closeCashModal: () => void;
  openTickerModal: () => void;
  closeTickerModal: () => void;
  openPfModal: () => void;
  closePfModal: () => void;
  openAlertModal: () => void;
  closeAlertModal: () => void;
}

export function useDashboardModals(): DashboardModals {
  const [showTxModal, setShowTxModal]         = useState(false);
  const [showCashModal, setShowCashModal]     = useState(false);
  const [showTickerModal, setShowTickerModal] = useState(false);
  const [showPfModal, setShowPfModal]         = useState(false);
  const [showAlertModal, setShowAlertModal]   = useState(false);

  return {
    showTxModal,
    showCashModal,
    showTickerModal,
    showPfModal,
    showAlertModal,
    openTxModal:      () => setShowTxModal(true),
    closeTxModal:     () => setShowTxModal(false),
    openCashModal:    () => setShowCashModal(true),
    closeCashModal:   () => setShowCashModal(false),
    openTickerModal:  () => setShowTickerModal(true),
    closeTickerModal: () => setShowTickerModal(false),
    openPfModal:      () => setShowPfModal(true),
    closePfModal:     () => setShowPfModal(false),
    openAlertModal:   () => setShowAlertModal(true),
    closeAlertModal:  () => setShowAlertModal(false),
  };
}
