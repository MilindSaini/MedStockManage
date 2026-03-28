import usePermissions from '../hooks/usePermissions';

export default function StockAdjustButton({ medicine, onAdjust, busy }) {
  const { canAdd, canSell } = usePermissions();

  return (
    <div className="flex items-center gap-2">
      <button
        type="button"
        className="rounded-md border border-rose-500/40 px-2 py-1 text-rose-300 disabled:opacity-40"
        disabled={busy || !canSell || medicine.currentStock <= 0}
        onClick={(event) => {
          event.stopPropagation();
          onAdjust(-1, 'SALE');
        }}
      >
        -
      </button>
      <button
        type="button"
        className="rounded-md border border-emerald-500/40 px-2 py-1 text-emerald-300 disabled:opacity-40"
        disabled={busy || !canAdd}
        onClick={(event) => {
          event.stopPropagation();
          onAdjust(1, 'RESTOCK');
        }}
      >
        +
      </button>
    </div>
  );
}
