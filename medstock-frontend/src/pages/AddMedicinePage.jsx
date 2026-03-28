import { useMemo, useState } from 'react';
import toast from 'react-hot-toast';
import { useLocation, useNavigate } from 'react-router-dom';
import useMedicines from '../hooks/useMedicines';
import usePermissions from '../hooks/usePermissions';

const OPTIONAL_TEXT_LIMIT = 120;
const SKU_PATTERN = /^[A-Za-z0-9][A-Za-z0-9-_/.]*$/;
const OTHER_CATEGORY = 'OTHER';
const CATEGORY_OPTIONS = [
  'Analgesic',
  'Antibiotic',
  'Antipyretic',
  'Antacid',
  'Antiseptic',
  'Antihistamine',
  'Vitamin',
  'Diabetes Care',
  'Cardiac Care',
  'Respiratory',
  'Dermatology',
  'Gastrointestinal',
];

function defaultForm() {
  return {
    name: '',
    genericName: '',
    categoryOption: '',
    customCategory: '',
    manufacturer: '',
    skuCode: '',
    unit: 'pcs',
    mrp: 0,
    purchasePrice: 0,
    quantityAvailable: 0,
    quantitySold: 0,
    lowStockThreshold: 10,
    expiryDate: '',
  };
}

function resolveCategoryFields(category) {
  const normalizedCategory = (category || '').trim();
  if (!normalizedCategory) {
    return { categoryOption: '', customCategory: '' };
  }

  if (CATEGORY_OPTIONS.includes(normalizedCategory)) {
    return { categoryOption: normalizedCategory, customCategory: '' };
  }

  return { categoryOption: OTHER_CATEGORY, customCategory: normalizedCategory };
}

export default function AddMedicinePage() {
  const navigate = useNavigate();
  const location = useLocation();
  const editingMedicine = location.state?.medicine || null;
  const { canAdd, canEdit, canDelete } = usePermissions();

  const initialValues = useMemo(() => {
    if (!editingMedicine) return defaultForm();

    const { categoryOption, customCategory } = resolveCategoryFields(editingMedicine.category);
    return {
      name: editingMedicine.name || '',
      genericName: editingMedicine.genericName || '',
      categoryOption,
      customCategory,
      manufacturer: editingMedicine.manufacturer || '',
      skuCode: editingMedicine.skuCode || '',
      unit: editingMedicine.unit || 'pcs',
      mrp: editingMedicine.mrp ?? 0,
      purchasePrice: editingMedicine.purchasePrice ?? 0,
      quantityAvailable: editingMedicine.quantityAvailable ?? 0,
      quantitySold: editingMedicine.quantitySold ?? 0,
      lowStockThreshold: editingMedicine.lowStockThreshold ?? 10,
      expiryDate: editingMedicine.expiryDate || '',
    };
  }, [editingMedicine]);

  const [form, setForm] = useState(initialValues);
  const [touched, setTouched] = useState({});
  const [errors, setErrors] = useState({});
  const { createMedicineMutation, updateMedicineMutation, deleteMedicineMutation } = useMedicines();

  const isEditing = Boolean(editingMedicine?.id);
  const canSubmit = isEditing ? canEdit : canAdd;
  const isSubmitting = createMedicineMutation.isPending || updateMedicineMutation.isPending;
  const isDeleting = deleteMedicineMutation.isPending;

  const fieldMeta = {
    name: { label: 'Medicine name', required: true },
    genericName: { label: 'Generic name' },
    categoryOption: { label: 'Category' },
    customCategory: { label: 'Custom category' },
    manufacturer: { label: 'Manufacturer' },
    skuCode: { label: 'SKU code' },
    unit: { label: 'Unit', required: true },
    mrp: { label: 'MRP', required: true },
    purchasePrice: { label: 'Purchase price', required: true },
    quantityAvailable: { label: 'Quantity available', required: true },
    quantitySold: { label: 'Quantity sold', required: true },
    lowStockThreshold: { label: 'Low stock threshold', required: true },
    expiryDate: { label: 'Expiry date' },
  };

  function sanitizeText(value) {
    return value.trim();
  }

  function asNumber(value) {
    if (value === '' || value === null || value === undefined) return NaN;
    return Number(value);
  }

  function resolveCategoryValue(currentForm) {
    if (currentForm.categoryOption === OTHER_CATEGORY) {
      return sanitizeText(currentForm.customCategory || '');
    }
    return sanitizeText(currentForm.categoryOption || '');
  }

  function validateField(key, value, currentForm = form) {
    const textValue = typeof value === 'string' ? sanitizeText(value) : value;

    switch (key) {
      case 'name': {
        if (!textValue) return 'Medicine name is required';
        if (textValue.length < 2) return 'Medicine name must be at least 2 characters';
        if (textValue.length > OPTIONAL_TEXT_LIMIT) return `Medicine name must be within ${OPTIONAL_TEXT_LIMIT} characters`;
        return '';
      }
      case 'genericName':
      case 'manufacturer': {
        if (textValue && textValue.length > OPTIONAL_TEXT_LIMIT) {
          return `${fieldMeta[key].label} must be within ${OPTIONAL_TEXT_LIMIT} characters`;
        }
        return '';
      }
      case 'categoryOption': {
        if (!value) return '';
        if (value === OTHER_CATEGORY) {
          const customCategory = sanitizeText(currentForm.customCategory || '');
          if (!customCategory) return 'Please type custom category';
        }
        if (value !== OTHER_CATEGORY && !CATEGORY_OPTIONS.includes(value)) {
          return 'Please select a valid category option';
        }
        return '';
      }
      case 'customCategory': {
        if (currentForm.categoryOption !== OTHER_CATEGORY) return '';
        if (!textValue) return 'Custom category is required';
        if (textValue.length > OPTIONAL_TEXT_LIMIT) {
          return `Custom category must be within ${OPTIONAL_TEXT_LIMIT} characters`;
        }
        return '';
      }
      case 'skuCode': {
        if (!textValue) return '';
        if (textValue.length > 40) return 'SKU code must be within 40 characters';
        if (!SKU_PATTERN.test(textValue)) return 'SKU code can use letters, numbers, -, _, /, and .';
        return '';
      }
      case 'unit': {
        if (!textValue) return 'Unit is required';
        if (textValue.length > 20) return 'Unit must be within 20 characters';
        return '';
      }
      case 'mrp': {
        const numeric = asNumber(value);
        if (Number.isNaN(numeric)) return 'MRP is required';
        if (numeric < 0) return 'MRP cannot be negative';
        return '';
      }
      case 'purchasePrice': {
        const numeric = asNumber(value);
        if (Number.isNaN(numeric)) return 'Purchase price is required';
        if (numeric < 0) return 'Purchase price cannot be negative';

        const mrp = asNumber(currentForm.mrp);
        if (!Number.isNaN(mrp) && numeric > mrp) return 'Purchase price cannot be greater than MRP';
        return '';
      }
      case 'quantityAvailable': {
        const numeric = asNumber(value);
        if (Number.isNaN(numeric)) return 'Quantity available is required';
        if (!Number.isInteger(numeric)) return 'Quantity available must be a whole number';
        if (numeric < 0) return 'Quantity available cannot be negative';
        return '';
      }
      case 'quantitySold': {
        const numeric = asNumber(value);
        if (Number.isNaN(numeric)) return 'Quantity sold is required';
        if (!Number.isInteger(numeric)) return 'Quantity sold must be a whole number';
        if (numeric < 0) return 'Quantity sold cannot be negative';
        return '';
      }
      case 'lowStockThreshold': {
        const numeric = asNumber(value);
        if (Number.isNaN(numeric)) return 'Low stock threshold is required';
        if (!Number.isInteger(numeric)) return 'Low stock threshold must be a whole number';
        if (numeric < 0) return 'Low stock threshold cannot be negative';
        return '';
      }
      case 'expiryDate': {
        if (!value) return '';
        const selectedDate = new Date(`${value}T00:00:00`);
        if (Number.isNaN(selectedDate.getTime())) return 'Please select a valid expiry date';
        return '';
      }
      default:
        return '';
    }
  }

  function validateForm(nextForm = form) {
    const nextErrors = {};
    Object.keys(fieldMeta).forEach((key) => {
      const error = validateField(key, nextForm[key], nextForm);
      if (error) nextErrors[key] = error;
    });
    return nextErrors;
  }

  function updateField(key, value) {
    setForm((prev) => {
      const nextForm = {
        ...prev,
        [key]: value,
      };

      if (key === 'categoryOption' && value !== OTHER_CATEGORY) {
        nextForm.customCategory = '';
      }

      if (touched[key] || key === 'mrp' || key === 'purchasePrice' || key === 'categoryOption' || key === 'customCategory') {
        setErrors((prevErrors) => {
          const nextErrors = { ...prevErrors };
          const fieldError = validateField(key, nextForm[key], nextForm);
          if (fieldError) {
            nextErrors[key] = fieldError;
          } else {
            delete nextErrors[key];
          }

          if (key === 'mrp' || key === 'purchasePrice' || touched.purchasePrice || touched.mrp) {
            const purchasePriceError = validateField('purchasePrice', nextForm.purchasePrice, nextForm);
            if (purchasePriceError) {
              nextErrors.purchasePrice = purchasePriceError;
            } else {
              delete nextErrors.purchasePrice;
            }
          }

          if (key === 'categoryOption' || key === 'customCategory' || touched.categoryOption || touched.customCategory) {
            const categoryOptionError = validateField('categoryOption', nextForm.categoryOption, nextForm);
            if (categoryOptionError) {
              nextErrors.categoryOption = categoryOptionError;
            } else {
              delete nextErrors.categoryOption;
            }

            const customCategoryError = validateField('customCategory', nextForm.customCategory, nextForm);
            if (customCategoryError) {
              nextErrors.customCategory = customCategoryError;
            } else {
              delete nextErrors.customCategory;
            }
          }

          return nextErrors;
        });
      }

      return nextForm;
    });
  }

  function onFieldBlur(key) {
    setTouched((prev) => ({ ...prev, [key]: true }));
    setErrors((prevErrors) => {
      const nextErrors = { ...prevErrors };
      const error = validateField(key, form[key], form);
      if (error) {
        nextErrors[key] = error;
      } else {
        delete nextErrors[key];
      }
      return nextErrors;
    });
  }

  function toPayload() {
    const categoryValue = resolveCategoryValue(form);

    return {
      name: form.name,
      genericName: form.genericName,
      category: categoryValue || null,
      manufacturer: form.manufacturer,
      skuCode: form.skuCode,
      unit: form.unit,
      mrp: Number(form.mrp),
      purchasePrice: Number(form.purchasePrice),
      quantityAvailable: Number(form.quantityAvailable),
      quantitySold: Number(form.quantitySold),
      lowStockThreshold: Number(form.lowStockThreshold),
      expiryDate: form.expiryDate || null,
    };
  }

  function onSubmit(event) {
    event.preventDefault();

    const nextTouched = Object.keys(fieldMeta).reduce((acc, key) => {
      acc[key] = true;
      return acc;
    }, {});
    setTouched(nextTouched);

    const nextErrors = validateForm(form);
    setErrors(nextErrors);

    if (Object.keys(nextErrors).length > 0) {
      toast.error('Please fix highlighted fields before saving');
      return;
    }

    if (!canSubmit) {
      toast.error('You do not have permission for this action');
      return;
    }

    const mutation = isEditing
      ? updateMedicineMutation.mutateAsync({ medicineId: editingMedicine.id, payload: toPayload() })
      : createMedicineMutation.mutateAsync(toPayload());

    mutation
      .then(() => {
        toast.success(isEditing ? 'Medicine updated' : 'Medicine created');
        navigate('/inventory');
      })
      .catch((error) => {
        toast.error(error.response?.data?.message || 'Could not save medicine');
      });
  }

  function onDeleteMedicine() {
    if (!isEditing || !editingMedicine?.id) {
      return;
    }

    if (!canDelete) {
      toast.error('You do not have permission to delete medicine');
      return;
    }

    const confirmed = window.confirm('Delete this medicine from active inventory?');
    if (!confirmed) {
      return;
    }

    deleteMedicineMutation.mutate(editingMedicine.id, {
      onSuccess: () => {
        toast.success('Medicine deleted');
        navigate('/inventory');
      },
      onError: (error) => {
        toast.error(error.response?.data?.message || 'Could not delete medicine');
      },
    });
  }

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 p-6 md:p-8">
      <div className="mx-auto max-w-4xl">
        <h1 className="text-3xl font-semibold">{isEditing ? 'Edit Medicine' : 'Add Medicine'}</h1>
        <p className="mt-1 text-sm text-slate-300">Core inventory form with all medicine fields used in stock and alerts.</p>

        <form className="mt-6 grid gap-4 md:grid-cols-2" onSubmit={onSubmit} noValidate>
          <label className="flex flex-col gap-1 text-sm">
            <span className="text-slate-200">Medicine name <span className="text-rose-400">*</span></span>
            <input
              className={`rounded-md border bg-slate-900 px-3 py-2 ${errors.name && touched.name ? 'border-rose-500 focus:border-rose-400' : 'border-slate-700'}`}
              placeholder="e.g. Crocin 500"
              value={form.name}
              onChange={(event) => updateField('name', event.target.value)}
              onBlur={() => onFieldBlur('name')}
              aria-invalid={Boolean(errors.name && touched.name)}
            />
            {errors.name && touched.name ? <span className="text-xs text-rose-400">{errors.name}</span> : null}
          </label>

          <label className="flex flex-col gap-1 text-sm">
            <span className="text-slate-200">Generic name</span>
            <input
              className={`rounded-md border bg-slate-900 px-3 py-2 ${errors.genericName && touched.genericName ? 'border-rose-500 focus:border-rose-400' : 'border-slate-700'}`}
              placeholder="e.g. Paracetamol"
              value={form.genericName}
              onChange={(event) => updateField('genericName', event.target.value)}
              onBlur={() => onFieldBlur('genericName')}
              aria-invalid={Boolean(errors.genericName && touched.genericName)}
            />
            {errors.genericName && touched.genericName ? <span className="text-xs text-rose-400">{errors.genericName}</span> : null}
          </label>

          <label className="flex flex-col gap-1 text-sm">
            <span className="text-slate-200">Category</span>
            <select
              className={`rounded-md border bg-slate-900 px-3 py-2 ${errors.categoryOption && touched.categoryOption ? 'border-rose-500 focus:border-rose-400' : 'border-slate-700'}`}
              value={form.categoryOption}
              onChange={(event) => updateField('categoryOption', event.target.value)}
              onBlur={() => onFieldBlur('categoryOption')}
              aria-invalid={Boolean(errors.categoryOption && touched.categoryOption)}
            >
              <option value="">Select category</option>
              {CATEGORY_OPTIONS.map((option) => (
                <option key={option} value={option}>{option}</option>
              ))}
              <option value={OTHER_CATEGORY}>Other</option>
            </select>
            {errors.categoryOption && touched.categoryOption ? <span className="text-xs text-rose-400">{errors.categoryOption}</span> : null}

            {form.categoryOption === OTHER_CATEGORY ? (
              <>
                <input
                  className={`mt-2 rounded-md border bg-slate-900 px-3 py-2 ${errors.customCategory && touched.customCategory ? 'border-rose-500 focus:border-rose-400' : 'border-slate-700'}`}
                  placeholder="Type category manually"
                  value={form.customCategory}
                  onChange={(event) => updateField('customCategory', event.target.value)}
                  onBlur={() => onFieldBlur('customCategory')}
                  aria-invalid={Boolean(errors.customCategory && touched.customCategory)}
                />
                {errors.customCategory && touched.customCategory ? <span className="text-xs text-rose-400">{errors.customCategory}</span> : null}
              </>
            ) : null}
          </label>

          <label className="flex flex-col gap-1 text-sm">
            <span className="text-slate-200">Manufacturer</span>
            <input
              className={`rounded-md border bg-slate-900 px-3 py-2 ${errors.manufacturer && touched.manufacturer ? 'border-rose-500 focus:border-rose-400' : 'border-slate-700'}`}
              placeholder="e.g. GSK"
              value={form.manufacturer}
              onChange={(event) => updateField('manufacturer', event.target.value)}
              onBlur={() => onFieldBlur('manufacturer')}
              aria-invalid={Boolean(errors.manufacturer && touched.manufacturer)}
            />
            {errors.manufacturer && touched.manufacturer ? <span className="text-xs text-rose-400">{errors.manufacturer}</span> : null}
          </label>

          <label className="flex flex-col gap-1 text-sm">
            <span className="text-slate-200">SKU code</span>
            <input
              className={`rounded-md border bg-slate-900 px-3 py-2 ${errors.skuCode && touched.skuCode ? 'border-rose-500 focus:border-rose-400' : 'border-slate-700'}`}
              placeholder="e.g. CROCIN-500"
              value={form.skuCode}
              onChange={(event) => updateField('skuCode', event.target.value)}
              onBlur={() => onFieldBlur('skuCode')}
              aria-invalid={Boolean(errors.skuCode && touched.skuCode)}
            />
            {errors.skuCode && touched.skuCode ? <span className="text-xs text-rose-400">{errors.skuCode}</span> : null}
          </label>

          <label className="flex flex-col gap-1 text-sm">
            <span className="text-slate-200">Unit <span className="text-rose-400">*</span></span>
            <input
              className={`rounded-md border bg-slate-900 px-3 py-2 ${errors.unit && touched.unit ? 'border-rose-500 focus:border-rose-400' : 'border-slate-700'}`}
              placeholder="pcs / strip / bottle"
              value={form.unit}
              onChange={(event) => updateField('unit', event.target.value)}
              onBlur={() => onFieldBlur('unit')}
              aria-invalid={Boolean(errors.unit && touched.unit)}
            />
            {errors.unit && touched.unit ? <span className="text-xs text-rose-400">{errors.unit}</span> : null}
          </label>

          <label className="flex flex-col gap-1 text-sm">
            <span className="text-slate-200">MRP <span className="text-rose-400">*</span></span>
            <input
              className={`rounded-md border bg-slate-900 px-3 py-2 ${errors.mrp && touched.mrp ? 'border-rose-500 focus:border-rose-400' : 'border-slate-700'}`}
              type="number"
              min="0"
              step="0.01"
              placeholder="0.00"
              value={form.mrp}
              onChange={(event) => updateField('mrp', event.target.value)}
              onBlur={() => onFieldBlur('mrp')}
              aria-invalid={Boolean(errors.mrp && touched.mrp)}
            />
            {errors.mrp && touched.mrp ? <span className="text-xs text-rose-400">{errors.mrp}</span> : null}
          </label>

          <label className="flex flex-col gap-1 text-sm">
            <span className="text-slate-200">Purchase price <span className="text-rose-400">*</span></span>
            <input
              className={`rounded-md border bg-slate-900 px-3 py-2 ${errors.purchasePrice && touched.purchasePrice ? 'border-rose-500 focus:border-rose-400' : 'border-slate-700'}`}
              type="number"
              min="0"
              step="0.01"
              placeholder="0.00"
              value={form.purchasePrice}
              onChange={(event) => updateField('purchasePrice', event.target.value)}
              onBlur={() => onFieldBlur('purchasePrice')}
              aria-invalid={Boolean(errors.purchasePrice && touched.purchasePrice)}
            />
            {errors.purchasePrice && touched.purchasePrice ? <span className="text-xs text-rose-400">{errors.purchasePrice}</span> : null}
          </label>

          <label className="flex flex-col gap-1 text-sm">
            <span className="text-slate-200">Quantity available <span className="text-rose-400">*</span></span>
            <input
              className={`rounded-md border bg-slate-900 px-3 py-2 ${errors.quantityAvailable && touched.quantityAvailable ? 'border-rose-500 focus:border-rose-400' : 'border-slate-700'}`}
              type="number"
              min="0"
              step="1"
              placeholder="0"
              value={form.quantityAvailable}
              onChange={(event) => updateField('quantityAvailable', event.target.value)}
              onBlur={() => onFieldBlur('quantityAvailable')}
              aria-invalid={Boolean(errors.quantityAvailable && touched.quantityAvailable)}
            />
            {errors.quantityAvailable && touched.quantityAvailable ? <span className="text-xs text-rose-400">{errors.quantityAvailable}</span> : null}
          </label>

          <label className="flex flex-col gap-1 text-sm">
            <span className="text-slate-200">Quantity sold <span className="text-rose-400">*</span></span>
            <input
              className={`rounded-md border bg-slate-900 px-3 py-2 ${errors.quantitySold && touched.quantitySold ? 'border-rose-500 focus:border-rose-400' : 'border-slate-700'}`}
              type="number"
              min="0"
              step="1"
              placeholder="0"
              value={form.quantitySold}
              onChange={(event) => updateField('quantitySold', event.target.value)}
              onBlur={() => onFieldBlur('quantitySold')}
              aria-invalid={Boolean(errors.quantitySold && touched.quantitySold)}
            />
            {errors.quantitySold && touched.quantitySold ? <span className="text-xs text-rose-400">{errors.quantitySold}</span> : null}
          </label>

          <label className="flex flex-col gap-1 text-sm">
            <span className="text-slate-200">Low stock threshold <span className="text-rose-400">*</span></span>
            <input
              className={`rounded-md border bg-slate-900 px-3 py-2 ${errors.lowStockThreshold && touched.lowStockThreshold ? 'border-rose-500 focus:border-rose-400' : 'border-slate-700'}`}
              type="number"
              min="0"
              step="1"
              placeholder="10"
              value={form.lowStockThreshold}
              onChange={(event) => updateField('lowStockThreshold', event.target.value)}
              onBlur={() => onFieldBlur('lowStockThreshold')}
              aria-invalid={Boolean(errors.lowStockThreshold && touched.lowStockThreshold)}
            />
            {errors.lowStockThreshold && touched.lowStockThreshold ? <span className="text-xs text-rose-400">{errors.lowStockThreshold}</span> : null}
          </label>

          <label className="flex flex-col gap-1 text-sm">
            <span className="text-slate-200">Expiry date</span>
            <input
              className={`rounded-md border bg-slate-900 px-3 py-2 ${errors.expiryDate && touched.expiryDate ? 'border-rose-500 focus:border-rose-400' : 'border-slate-700'}`}
              type="date"
              value={form.expiryDate}
              onChange={(event) => updateField('expiryDate', event.target.value)}
              onBlur={() => onFieldBlur('expiryDate')}
              aria-invalid={Boolean(errors.expiryDate && touched.expiryDate)}
            />
            {errors.expiryDate && touched.expiryDate ? <span className="text-xs text-rose-400">{errors.expiryDate}</span> : null}
          </label>

          <div className="md:col-span-2 mt-2 flex gap-3">
            <button
              type="submit"
              disabled={!canSubmit || isSubmitting || isDeleting}
              className="rounded-md bg-emerald-600 px-4 py-2 font-medium hover:bg-emerald-500 disabled:opacity-40"
            >
              {isSubmitting ? 'Saving...' : isEditing ? 'Update Medicine' : 'Create Medicine'}
            </button>
            {isEditing ? (
              <button
                type="button"
                onClick={onDeleteMedicine}
                disabled={!canDelete || isSubmitting || isDeleting}
                className="rounded-md border border-rose-500/60 px-4 py-2 text-rose-300 disabled:opacity-40"
              >
                {isDeleting ? 'Deleting...' : 'Delete Medicine'}
              </button>
            ) : null}
            <button
              type="button"
              onClick={() => navigate('/inventory')}
              disabled={isDeleting}
              className="rounded-md border border-slate-600 px-4 py-2"
            >
              Cancel
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
