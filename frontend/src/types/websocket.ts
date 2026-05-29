export interface MarketUpdate {
  ticker: string;
  price: number;
}

export interface TriggeredAlert {
  priceAlertId?: number;
  alertId?: number;
  ticker?: string;
  assetId?: number;
  targetPrice: number;
  currentPrice: number;
}
