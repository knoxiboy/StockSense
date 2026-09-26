import React, { useState, useEffect, useCallback } from 'react';
import {
  Plus,
  Search,
  Edit2,
  Trash2,
  AlertTriangle,
  RotateCcw,
} from 'lucide-react';
import { productApi } from '../api/products';
import { Product, CreateProductDto, UpdateProductDto } from '../types/product';
import Modal from '../components/ui/Modal';
import Badge from '../components/ui/Badge';
import LoadingSpinner from '../components/common/LoadingSpinner';
import EmptyState from '../components/common/EmptyState';
import { formatCurrency, formatQuantity } from '../utils/formatters';

interface ProductsPageProps {
  onSuccessToast: (msg: string) => void;
  onErrorToast: (msg: string) => void;
  onUpdateTotalCount?: (count: number) => void;
}

interface FormErrors {
  name?: string;
  sku?: string;
  unitOfMeasure?: string;
  reorderLevel?: string;
  price?: string;
  general?: string;
}

export const ProductsPage: React.FC<ProductsPageProps> = ({
  onSuccessToast,
  onErrorToast,
  onUpdateTotalCount,
}) => {
  const [products, setProducts] = useState<Product[]>([]);
  const [categories, setCategories] = useState<string[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [searchTerm, setSearchTerm] = useState<string>('');
  const [selectedCategory, setSelectedCategory] = useState<string>('');

  // Modal states
  const [isCreateModalOpen, setIsCreateModalOpen] = useState<boolean>(false);
  const [isEditModalOpen, setIsEditModalOpen] = useState<boolean>(false);
  const [isDeleteModalOpen, setIsDeleteModalOpen] = useState<boolean>(false);
  const [activeProduct, setActiveProduct] = useState<Product | null>(null);

  // Form states
  const [formData, setFormData] = useState({
    name: '',
    sku: '',
    category: '',
    description: '',
    unitOfMeasure: 'units',
    reorderLevel: '0',
    price: '',
    active: true,
  });
  const [formErrors, setFormErrors] = useState<FormErrors>({});
  const [isSubmitting, setIsSubmitting] = useState<boolean>(false);

  const loadData = useCallback(async () => {
    setLoading(true);
    try {
      const [productList, catList] = await Promise.all([
        productApi.getAll(searchTerm, selectedCategory),
        productApi.getCategories(),
      ]);
      setProducts(productList);
      setCategories(catList);
      if (onUpdateTotalCount) {
        onUpdateTotalCount(productList.length);
      }
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Failed to load products';
      onErrorToast(msg);
    } finally {
      setLoading(false);
    }
  }, [searchTerm, selectedCategory, onErrorToast, onUpdateTotalCount]);

  useEffect(() => {
    const handler = setTimeout(() => {
      loadData();
    }, 250);
    return () => clearTimeout(handler);
  }, [loadData]);

  const resetForm = () => {
    setFormData({
      name: '',
      sku: '',
      category: '',
      description: '',
      unitOfMeasure: 'units',
      reorderLevel: '0',
      price: '',
      active: true,
    });
    setFormErrors({});
  };

  const handleOpenCreate = () => {
    resetForm();
    setIsCreateModalOpen(true);
  };

  const handleOpenEdit = (product: Product) => {
    setActiveProduct(product);
    setFormData({
      name: product.name,
      sku: product.sku,
      category: product.category || '',
      description: product.description || '',
      unitOfMeasure: product.unitOfMeasure,
      reorderLevel: String(product.reorderLevel),
      price: product.price !== null && product.price !== undefined ? String(product.price) : '',
      active: product.active,
    });
    setFormErrors({});
    setIsEditModalOpen(true);
  };

  const handleOpenDelete = (product: Product) => {
    setActiveProduct(product);
    setIsDeleteModalOpen(true);
  };

  const validateForm = (): boolean => {
    const errors: FormErrors = {};
    if (!formData.name.trim()) errors.name = 'Product name is required';
    if (!formData.sku.trim()) errors.sku = 'SKU is required';
    if (!formData.unitOfMeasure.trim()) errors.unitOfMeasure = 'Unit of measure is required';

    const reorder = parseFloat(formData.reorderLevel);
    if (isNaN(reorder) || reorder < 0) {
      errors.reorderLevel = 'Reorder level must be a non-negative number';
    }

    if (formData.price && formData.price.trim() !== '') {
      const p = parseFloat(formData.price);
      if (isNaN(p) || p < 0) {
        errors.price = 'Price must be a non-negative number';
      }
    }

    setFormErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const handleCreateSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!validateForm()) return;

    setIsSubmitting(true);
    try {
      const payload: CreateProductDto = {
        name: formData.name.trim(),
        sku: formData.sku.trim(),
        category: formData.category.trim() || undefined,
        description: formData.description.trim() || undefined,
        unitOfMeasure: formData.unitOfMeasure.trim(),
        reorderLevel: formData.reorderLevel,
        price: formData.price.trim() !== '' ? formData.price : null,
      };

      await productApi.create(payload);
      onSuccessToast(`Product "${payload.name}" created successfully`);
      setIsCreateModalOpen(false);
      resetForm();
      loadData();
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Error creating product';
      onErrorToast(msg);
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleEditSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!activeProduct || !validateForm()) return;

    setIsSubmitting(true);
    try {
      const payload: UpdateProductDto = {
        name: formData.name.trim(),
        sku: formData.sku.trim(),
        category: formData.category.trim() || undefined,
        description: formData.description.trim() || undefined,
        unitOfMeasure: formData.unitOfMeasure.trim(),
        reorderLevel: formData.reorderLevel,
        price: formData.price.trim() !== '' ? formData.price : null,
        active: formData.active,
      };

      await productApi.update(activeProduct.id, payload);
      onSuccessToast(`Product "${payload.name}" updated successfully`);
      setIsEditModalOpen(false);
      resetForm();
      loadData();
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Error updating product';
      onErrorToast(msg);
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleDeleteConfirm = async () => {
    if (!activeProduct) return;

    setIsSubmitting(true);
    try {
      await productApi.delete(activeProduct.id);
      onSuccessToast(`Product "${activeProduct.name}" removed successfully`);
      setIsDeleteModalOpen(false);
      setActiveProduct(null);
      loadData();
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Error deleting product';
      onErrorToast(msg);
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div>
      {/* Search and Action Toolbar */}
      <div className="card toolbar-card">
        <div style={{ display: 'flex', alignItems: 'center', gap: '12px', flexWrap: 'wrap' }}>
          <div className="search-box">
            <Search size={16} color="var(--text-muted)" />
            <input
              type="text"
              placeholder="Search by SKU or name..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
            />
          </div>

          <select
            className="filter-select"
            value={selectedCategory}
            onChange={(e) => setSelectedCategory(e.target.value)}
          >
            <option value="">All Categories</option>
            {categories.map((cat) => (
              <option key={cat} value={cat}>
                {cat}
              </option>
            ))}
          </select>

          {(searchTerm || selectedCategory) && (
            <button
              type="button"
              className="btn btn-secondary btn-sm"
              onClick={() => {
                setSearchTerm('');
                setSelectedCategory('');
              }}
            >
              <RotateCcw size={14} />
              Reset Filters
            </button>
          )}
        </div>

        <button type="button" className="btn btn-primary" onClick={handleOpenCreate}>
          <Plus size={16} />
          <span>New Product</span>
        </button>
      </div>

      {/* Products Table Card */}
      <div className="card">
        {loading ? (
          <LoadingSpinner message="Fetching product catalog..." />
        ) : products.length === 0 ? (
          <EmptyState
            title="No Products Found"
            description={
              searchTerm || selectedCategory
                ? 'No products matched your search or category filters. Try adjusting them.'
                : 'Get started by creating your first product item in the inventory catalog.'
            }
            actionText={searchTerm || selectedCategory ? undefined : 'Add First Product'}
            onAction={handleOpenCreate}
          />
        ) : (
          <div className="table-container">
            <table className="custom-table">
              <thead>
                <tr>
                  <th>Product & SKU</th>
                  <th>Category</th>
                  <th>Unit</th>
                  <th>Reorder Level</th>
                  <th>Unit Price</th>
                  <th>Status</th>
                  <th style={{ textAlign: 'right' }}>Actions</th>
                </tr>
              </thead>
              <tbody>
                {products.map((p) => (
                  <tr key={p.id}>
                    <td>
                      <div style={{ fontWeight: 600, color: 'var(--text-main)' }}>{p.name}</div>
                      <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', fontFamily: 'monospace' }}>
                        {p.sku}
                      </div>
                    </td>
                    <td>
                      {p.category ? (
                        <Badge variant="secondary">{p.category}</Badge>
                      ) : (
                        <span style={{ color: 'var(--text-light)' }}>—</span>
                      )}
                    </td>
                    <td>
                      <span style={{ fontSize: '0.85rem' }}>{p.unitOfMeasure}</span>
                    </td>
                    <td>
                      <span style={{ fontWeight: 500 }}>
                        {formatQuantity(p.reorderLevel, p.unitOfMeasure)}
                      </span>
                    </td>
                    <td>
                      <span style={{ fontWeight: 500 }}>
                        {formatCurrency(p.price)}
                      </span>
                    </td>
                    <td>
                      {p.active ? (
                        <Badge variant="success">Active</Badge>
                      ) : (
                        <Badge variant="secondary">Archived</Badge>
                      )}
                    </td>
                    <td style={{ textAlign: 'right' }}>
                      <div style={{ display: 'inline-flex', gap: '6px' }}>
                        <button
                          type="button"
                          className="btn-icon-only"
                          title="Edit Product"
                          onClick={() => handleOpenEdit(p)}
                        >
                          <Edit2 size={16} />
                        </button>
                        <button
                          type="button"
                          className="btn-icon-only"
                          style={{ color: 'var(--danger)' }}
                          title="Delete Product"
                          onClick={() => handleOpenDelete(p)}
                        >
                          <Trash2 size={16} />
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Create Product Modal */}
      <Modal
        isOpen={isCreateModalOpen}
        onClose={() => setIsCreateModalOpen(false)}
        title="Add New Product"
        footer={
          <>
            <button
              type="button"
              className="btn btn-secondary"
              onClick={() => setIsCreateModalOpen(false)}
              disabled={isSubmitting}
            >
              Cancel
            </button>
            <button
              type="button"
              className="btn btn-primary"
              onClick={handleCreateSubmit}
              disabled={isSubmitting}
            >
              {isSubmitting ? 'Creating...' : 'Create Product'}
            </button>
          </>
        }
      >
        <form onSubmit={handleCreateSubmit}>
          <div className="form-group">
            <label className="form-label">
              Product Name <span className="required">*</span>
            </label>
            <input
              type="text"
              className={`form-control ${formErrors.name ? 'error' : ''}`}
              placeholder="e.g. Steel Rods 10mm"
              value={formData.name}
              onChange={(e) => setFormData({ ...formData, name: e.target.value })}
            />
            {formErrors.name && <div className="field-error">{formErrors.name}</div>}
          </div>

          <div className="form-row">
            <div className="form-group">
              <label className="form-label">
                SKU / Code <span className="required">*</span>
              </label>
              <input
                type="text"
                className={`form-control ${formErrors.sku ? 'error' : ''}`}
                placeholder="e.g. STL-10MM-01"
                value={formData.sku}
                onChange={(e) => setFormData({ ...formData, sku: e.target.value })}
              />
              {formErrors.sku && <div className="field-error">{formErrors.sku}</div>}
            </div>

            <div className="form-group">
              <label className="form-label">Category</label>
              <input
                type="text"
                className="form-control"
                placeholder="e.g. Raw Materials"
                value={formData.category}
                onChange={(e) => setFormData({ ...formData, category: e.target.value })}
              />
            </div>
          </div>

          <div className="form-row">
            <div className="form-group">
              <label className="form-label">
                Unit of Measure <span className="required">*</span>
              </label>
              <select
                className="form-control"
                value={formData.unitOfMeasure}
                onChange={(e) => setFormData({ ...formData, unitOfMeasure: e.target.value })}
              >
                <option value="units">units</option>
                <option value="pcs">pcs</option>
                <option value="kg">kg</option>
                <option value="meters">meters</option>
                <option value="liters">liters</option>
                <option value="boxes">boxes</option>
                <option value="cartons">cartons</option>
              </select>
              {formErrors.unitOfMeasure && <div className="field-error">{formErrors.unitOfMeasure}</div>}
            </div>

            <div className="form-group">
              <label className="form-label">
                Reorder Level <span className="required">*</span>
              </label>
              <input
                type="number"
                step="any"
                min="0"
                className={`form-control ${formErrors.reorderLevel ? 'error' : ''}`}
                placeholder="e.g. 10.00"
                value={formData.reorderLevel}
                onChange={(e) => setFormData({ ...formData, reorderLevel: e.target.value })}
              />
              {formErrors.reorderLevel && <div className="field-error">{formErrors.reorderLevel}</div>}
            </div>
          </div>

          <div className="form-group">
            <label className="form-label">Unit Price ($)</label>
            <input
              type="number"
              step="0.01"
              min="0"
              className={`form-control ${formErrors.price ? 'error' : ''}`}
              placeholder="e.g. 25.50"
              value={formData.price}
              onChange={(e) => setFormData({ ...formData, price: e.target.value })}
            />
            {formErrors.price && <div className="field-error">{formErrors.price}</div>}
          </div>

          <div className="form-group">
            <label className="form-label">Description</label>
            <textarea
              rows={3}
              className="form-control"
              placeholder="Detailed specifications, vendor references, or notes..."
              value={formData.description}
              onChange={(e) => setFormData({ ...formData, description: e.target.value })}
            />
          </div>
        </form>
      </Modal>

      {/* Edit Product Modal */}
      <Modal
        isOpen={isEditModalOpen}
        onClose={() => setIsEditModalOpen(false)}
        title="Edit Product"
        footer={
          <>
            <button
              type="button"
              className="btn btn-secondary"
              onClick={() => setIsEditModalOpen(false)}
              disabled={isSubmitting}
            >
              Cancel
            </button>
            <button
              type="button"
              className="btn btn-primary"
              onClick={handleEditSubmit}
              disabled={isSubmitting}
            >
              {isSubmitting ? 'Saving...' : 'Save Changes'}
            </button>
          </>
        }
      >
        <form onSubmit={handleEditSubmit}>
          <div className="form-group">
            <label className="form-label">
              Product Name <span className="required">*</span>
            </label>
            <input
              type="text"
              className={`form-control ${formErrors.name ? 'error' : ''}`}
              value={formData.name}
              onChange={(e) => setFormData({ ...formData, name: e.target.value })}
            />
            {formErrors.name && <div className="field-error">{formErrors.name}</div>}
          </div>

          <div className="form-row">
            <div className="form-group">
              <label className="form-label">
                SKU / Code <span className="required">*</span>
              </label>
              <input
                type="text"
                className={`form-control ${formErrors.sku ? 'error' : ''}`}
                value={formData.sku}
                onChange={(e) => setFormData({ ...formData, sku: e.target.value })}
              />
              {formErrors.sku && <div className="field-error">{formErrors.sku}</div>}
            </div>

            <div className="form-group">
              <label className="form-label">Category</label>
              <input
                type="text"
                className="form-control"
                value={formData.category}
                onChange={(e) => setFormData({ ...formData, category: e.target.value })}
              />
            </div>
          </div>

          <div className="form-row">
            <div className="form-group">
              <label className="form-label">
                Unit of Measure <span className="required">*</span>
              </label>
              <select
                className="form-control"
                value={formData.unitOfMeasure}
                onChange={(e) => setFormData({ ...formData, unitOfMeasure: e.target.value })}
              >
                <option value="units">units</option>
                <option value="pcs">pcs</option>
                <option value="kg">kg</option>
                <option value="meters">meters</option>
                <option value="liters">liters</option>
                <option value="boxes">boxes</option>
                <option value="cartons">cartons</option>
              </select>
              {formErrors.unitOfMeasure && <div className="field-error">{formErrors.unitOfMeasure}</div>}
            </div>

            <div className="form-group">
              <label className="form-label">
                Reorder Level <span className="required">*</span>
              </label>
              <input
                type="number"
                step="any"
                min="0"
                className={`form-control ${formErrors.reorderLevel ? 'error' : ''}`}
                value={formData.reorderLevel}
                onChange={(e) => setFormData({ ...formData, reorderLevel: e.target.value })}
              />
              {formErrors.reorderLevel && <div className="field-error">{formErrors.reorderLevel}</div>}
            </div>
          </div>

          <div className="form-row">
            <div className="form-group">
              <label className="form-label">Unit Price ($)</label>
              <input
                type="number"
                step="0.01"
                min="0"
                className={`form-control ${formErrors.price ? 'error' : ''}`}
                value={formData.price}
                onChange={(e) => setFormData({ ...formData, price: e.target.value })}
              />
              {formErrors.price && <div className="field-error">{formErrors.price}</div>}
            </div>

            <div className="form-group" style={{ display: 'flex', flexDirection: 'column', justifyContent: 'center' }}>
              <label className="form-label">Record Status</label>
              <label style={{ display: 'flex', alignItems: 'center', gap: '8px', cursor: 'pointer', marginTop: '6px' }}>
                <input
                  type="checkbox"
                  checked={formData.active}
                  onChange={(e) => setFormData({ ...formData, active: e.target.checked })}
                />
                <span style={{ fontSize: '0.875rem' }}>Active in Catalog</span>
              </label>
            </div>
          </div>

          <div className="form-group">
            <label className="form-label">Description</label>
            <textarea
              rows={3}
              className="form-control"
              value={formData.description}
              onChange={(e) => setFormData({ ...formData, description: e.target.value })}
            />
          </div>
        </form>
      </Modal>

      {/* Delete Confirmation Modal */}
      <Modal
        isOpen={isDeleteModalOpen}
        onClose={() => setIsDeleteModalOpen(false)}
        title="Confirm Delete"
        footer={
          <>
            <button
              type="button"
              className="btn btn-secondary"
              onClick={() => setIsDeleteModalOpen(false)}
              disabled={isSubmitting}
            >
              Cancel
            </button>
            <button
              type="button"
              className="btn btn-danger"
              onClick={handleDeleteConfirm}
              disabled={isSubmitting}
            >
              {isSubmitting ? 'Deleting...' : 'Delete Product'}
            </button>
          </>
        }
      >
        <div style={{ display: 'flex', gap: '16px', alignItems: 'flex-start' }}>
          <div style={{ color: 'var(--danger)', marginTop: '2px' }}>
            <AlertTriangle size={24} />
          </div>
          <div>
            <p style={{ fontSize: '0.95rem', marginBottom: '8px', color: 'var(--text-main)' }}>
              Are you sure you want to delete product{' '}
              <strong>"{activeProduct?.name}"</strong> (SKU: <code>{activeProduct?.sku}</code>)?
            </p>
            <p style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>
              This will remove the product definition from the catalog. This action cannot be undone.
            </p>
          </div>
        </div>
      </Modal>
    </div>
  );
};

export default ProductsPage;
