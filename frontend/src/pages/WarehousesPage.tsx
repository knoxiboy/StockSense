import React, { useState, useEffect, useCallback } from 'react';
import { Warehouse as WarehouseIcon, MapPin, Plus, Edit2, CheckCircle2, XCircle, Search } from 'lucide-react';
import { warehouseApi, locationApi } from '../api/warehouses';
import { Warehouse, Location, LocationType } from '../types/warehouse';
import LoadingSpinner from '../components/common/LoadingSpinner';
import EmptyState from '../components/common/EmptyState';
import Badge from '../components/ui/Badge';
import Modal from '../components/ui/Modal';


interface WarehousesPageProps {
  onSuccessToast?: (msg: string) => void;
  onErrorToast?: (msg: string) => void;
}

export const WarehousesPage: React.FC<WarehousesPageProps> = ({
  onSuccessToast = () => {},
  onErrorToast = () => {},
}) => {
  const [warehouses, setWarehouses] = useState<Warehouse[]>([]);
  const [locations, setLocations] = useState<Location[]>([]);
  const [selectedWarehouseId, setSelectedWarehouseId] = useState<number | null>(null);
  const [loading, setLoading] = useState<boolean>(true);
  const [search, setSearch] = useState<string>('');

  // Warehouse Modal
  const [isWarehouseModalOpen, setIsWarehouseModalOpen] = useState(false);
  const [whName, setWhName] = useState('');
  const [whCode, setWhCode] = useState('');
  const [whAddress, setWhAddress] = useState('');
  const [editingWhId, setEditingWhId] = useState<number | null>(null);
  const [whSubmitting, setWhSubmitting] = useState(false);

  // Location Modal
  const [isLocationModalOpen, setIsLocationModalOpen] = useState(false);
  const [locName, setLocName] = useState('');
  const [locCode, setLocCode] = useState('');
  const [locWarehouseId, setLocWarehouseId] = useState<number>(0);
  const [locType, setLocType] = useState<LocationType>('INTERNAL');
  const [editingLocId, setEditingLocId] = useState<number | null>(null);
  const [locSubmitting, setLocSubmitting] = useState(false);

  const fetchData = useCallback(async () => {
    setLoading(true);
    try {
      const [whList, locList] = await Promise.all([
        warehouseApi.getAll(),
        locationApi.getAll(),
      ]);
      setWarehouses(whList);
      setLocations(locList);
      if (whList.length > 0 && selectedWarehouseId === null) {
        setSelectedWarehouseId(whList[0].id);
      }
    } catch (err: any) {
      onErrorToast(err.message || 'Failed to load warehouses');
    } finally {
      setLoading(false);
    }
  }, [selectedWarehouseId, onErrorToast]);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  const handleSaveWarehouse = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!whName.trim() || !whCode.trim()) {
      onErrorToast('Name and code are required');
      return;
    }

    setWhSubmitting(true);
    try {
      if (editingWhId) {
        await warehouseApi.update(editingWhId, {
          name: whName.trim(),
          code: whCode.trim(),
          address: whAddress.trim() || undefined,
        });
        onSuccessToast('Warehouse updated successfully');
      } else {
        await warehouseApi.create({
          name: whName.trim(),
          code: whCode.trim(),
          address: whAddress.trim() || undefined,
        });
        onSuccessToast('Warehouse created successfully');
      }
      setIsWarehouseModalOpen(false);
      fetchData();
    } catch (err: any) {
      onErrorToast(err.message || 'Failed to save warehouse');
    } finally {
      setWhSubmitting(false);
    }
  };

  const handleSaveLocation = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!locName.trim() || !locCode.trim() || !locWarehouseId) {
      onErrorToast('Name, code, and warehouse are required');
      return;
    }

    setLocSubmitting(true);
    try {
      if (editingLocId) {
        await locationApi.update(editingLocId, {
          name: locName.trim(),
          code: locCode.trim(),
          warehouseId: locWarehouseId,
          locationType: locType,
        });
        onSuccessToast('Location updated successfully');
      } else {
        await locationApi.create({
          name: locName.trim(),
          code: locCode.trim(),
          warehouseId: locWarehouseId,
          locationType: locType,
        });
        onSuccessToast('Location created successfully');
      }
      setIsLocationModalOpen(false);
      fetchData();
    } catch (err: any) {
      onErrorToast(err.message || 'Failed to save location');
    } finally {
      setLocSubmitting(false);
    }
  };

  const filteredLocations = locations.filter((loc) => {
    const matchesWh = selectedWarehouseId === null || loc.warehouseId === selectedWarehouseId;
    const matchesSearch =
      loc.name.toLowerCase().includes(search.toLowerCase()) ||
      loc.code.toLowerCase().includes(search.toLowerCase());
    return matchesWh && matchesSearch;
  });

  if (loading && warehouses.length === 0) {
    return (
      <div style={{ display: 'flex', justifyContent: 'center', padding: '80px 0' }}>
        <LoadingSpinner message="Loading warehouses and storage locations..." />
      </div>
    );
  }

  return (
    <div className="warehouses-page-container">
      {/* Header Actions */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '24px', flexWrap: 'wrap', gap: '16px' }}>
        <div>
          <h2 style={{ fontSize: '1.25rem', fontWeight: 700 }}>Warehouse Network</h2>
          <p style={{ color: 'var(--text-muted)', fontSize: '0.85rem' }}>
            Manage physical warehouses, zones, storage bins, and location stock balances
          </p>
        </div>
        <div style={{ display: 'flex', gap: '12px' }}>
          <button
            type="button"
            className="btn btn-secondary"
            onClick={() => {
              setEditingWhId(null);
              setWhName('');
              setWhCode('');
              setWhAddress('');
              setIsWarehouseModalOpen(true);
            }}
          >
            <WarehouseIcon size={16} />
            <span>New Warehouse</span>
          </button>
          <button
            type="button"
            className="btn btn-primary"
            onClick={() => {
              setEditingLocId(null);
              setLocName('');
              setLocCode('');
              setLocWarehouseId(selectedWarehouseId || (warehouses[0]?.id ?? 0));
              setLocType('INTERNAL');
              setIsLocationModalOpen(true);
            }}
          >
            <Plus size={16} />
            <span>New Location</span>
          </button>
        </div>
      </div>

      {/* Warehouse Selector Cards */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(280px, 1fr))', gap: '16px', marginBottom: '28px' }}>
        {warehouses.map((wh) => {
          const isSelected = selectedWarehouseId === wh.id;
          const whLocs = locations.filter((l) => l.warehouseId === wh.id);

          return (
            <div
              key={wh.id}
              className={`card ${isSelected ? 'selected-card' : ''}`}
              style={{
                padding: '18px 20px',
                cursor: 'pointer',
                border: isSelected ? '2px solid var(--primary)' : '1px solid var(--border-subtle)',
                background: isSelected ? 'rgba(59, 130, 246, 0.04)' : 'var(--surface)',
                transition: 'all 0.2s ease',
              }}
              onClick={() => setSelectedWarehouseId(wh.id)}
            >
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '8px' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                  <WarehouseIcon size={18} style={{ color: isSelected ? 'var(--primary)' : 'var(--text-muted)' }} />
                  <span style={{ fontWeight: 700, fontSize: '1rem' }}>{wh.name}</span>
                </div>
                <button
                  type="button"
                  className="btn btn-secondary btn-sm"
                  style={{ padding: '4px 8px' }}
                  onClick={(e) => {
                    e.stopPropagation();
                    setEditingWhId(wh.id);
                    setWhName(wh.name);
                    setWhCode(wh.code);
                    setWhAddress(wh.address || '');
                    setIsWarehouseModalOpen(true);
                  }}
                >
                  <Edit2 size={13} />
                </button>
              </div>

              <div style={{ fontSize: '0.8rem', color: 'var(--text-muted)', marginBottom: '12px' }}>
                Code: <code style={{ fontWeight: 600 }}>{wh.code}</code> • {wh.address || 'No address specified'}
              </div>

              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', fontSize: '0.8rem' }}>
                <span style={{ color: 'var(--text-muted)' }}>{whLocs.length} Storage Locations</span>
                <Badge variant={wh.active ? 'success' : 'secondary'}>
                  {wh.active ? 'Operational' : 'Inactive'}
                </Badge>
              </div>
            </div>
          );
        })}
      </div>

      {/* Locations Section */}
      <div className="card" style={{ padding: '20px' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '18px', flexWrap: 'wrap', gap: '12px' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <MapPin size={18} style={{ color: 'var(--primary)' }} />
            <h3 style={{ fontSize: '1rem', fontWeight: 600 }}>
              Storage Locations {selectedWarehouseId && `(${warehouses.find(w => w.id === selectedWarehouseId)?.name || ''})`}
            </h3>
          </div>
          <div style={{ display: 'flex', gap: '10px', alignItems: 'center' }}>
            <div className="search-input-wrap" style={{ position: 'relative' }}>
              <Search size={15} style={{ position: 'absolute', left: '10px', top: '50%', transform: 'translateY(-50%)', color: 'var(--text-muted)' }} />
              <input
                type="text"
                className="form-control"
                placeholder="Search locations..."
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                style={{ paddingLeft: '32px', width: '220px', height: '36px' }}
              />
            </div>
            <button
              type="button"
              className="btn btn-secondary btn-sm"
              onClick={() => setSelectedWarehouseId(null)}
              style={{ height: '36px' }}
            >
              Show All Warehouses
            </button>
          </div>
        </div>

        {filteredLocations.length === 0 ? (
          <EmptyState
            title="No locations found"
            description="Create your first storage rack, shelf, or bin in this warehouse."
            actionText="Create Location"
            onAction={() => {
              setEditingLocId(null);
              setLocName('');
              setLocCode('');
              setLocWarehouseId(selectedWarehouseId || (warehouses[0]?.id ?? 0));
              setLocType('INTERNAL');
              setIsLocationModalOpen(true);
            }}
          />
        ) : (
          <div className="table-container">
            <table className="custom-table">
              <thead>
                <tr>
                  <th>Location Name</th>
                  <th>Location Code</th>
                  <th>Warehouse</th>
                  <th>Type</th>
                  <th>Status</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {filteredLocations.map((loc) => (
                  <tr key={loc.id}>
                    <td>
                      <div style={{ fontWeight: 600, display: 'flex', alignItems: 'center', gap: '6px' }}>
                        <MapPin size={14} style={{ color: 'var(--primary)' }} />
                        {loc.name}
                      </div>
                    </td>
                    <td>
                      <code>{loc.code}</code>
                    </td>
                    <td>{loc.warehouseName || 'Main Warehouse'}</td>
                    <td>
                      <Badge variant={loc.locationType === 'INTERNAL' ? 'info' : 'warning'}>
                        {loc.locationType}
                      </Badge>
                    </td>
                    <td>
                      <span style={{ display: 'flex', alignItems: 'center', gap: '4px', fontSize: '0.8rem', color: loc.active ? 'var(--success)' : 'var(--text-muted)' }}>
                        {loc.active ? <CheckCircle2 size={14} /> : <XCircle size={14} />}
                        {loc.active ? 'Active' : 'Disabled'}
                      </span>
                    </td>
                    <td>
                      <button
                        type="button"
                        className="btn btn-secondary btn-sm"
                        onClick={() => {
                          setEditingLocId(loc.id);
                          setLocName(loc.name);
                          setLocCode(loc.code);
                          setLocWarehouseId(loc.warehouseId);
                          setLocType(loc.locationType);
                          setIsLocationModalOpen(true);
                        }}
                      >
                        <Edit2 size={13} />
                        <span>Edit</span>
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Warehouse Modal */}
      {isWarehouseModalOpen && (
        <Modal
          isOpen={isWarehouseModalOpen}
          title={editingWhId ? 'Edit Warehouse' : 'New Warehouse'}
          onClose={() => setIsWarehouseModalOpen(false)}
        >
          <form onSubmit={handleSaveWarehouse}>
            <div className="form-group" style={{ marginBottom: '14px' }}>
              <label className="form-label">Warehouse Name *</label>
              <input
                type="text"
                className="form-control"
                required
                value={whName}
                onChange={(e) => setWhName(e.target.value)}
                placeholder="e.g. Central Distribution Center"
              />
            </div>
            <div className="form-group" style={{ marginBottom: '14px' }}>
              <label className="form-label">Unique Code *</label>
              <input
                type="text"
                className="form-control"
                required
                value={whCode}
                onChange={(e) => setWhCode(e.target.value.toUpperCase())}
                placeholder="e.g. WH-CENTRAL"
              />
            </div>
            <div className="form-group" style={{ marginBottom: '20px' }}>
              <label className="form-label">Physical Address</label>
              <textarea
                className="form-control"
                rows={2}
                value={whAddress}
                onChange={(e) => setWhAddress(e.target.value)}
                placeholder="e.g. 500 Industrial Pkwy, Sector 4"
              />
            </div>
            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '10px' }}>
              <button
                type="button"
                className="btn btn-secondary"
                onClick={() => setIsWarehouseModalOpen(false)}
                disabled={whSubmitting}
              >
                Cancel
              </button>
              <button type="submit" className="btn btn-primary" disabled={whSubmitting}>
                {whSubmitting ? 'Saving...' : editingWhId ? 'Update' : 'Create Warehouse'}
              </button>
            </div>
          </form>
        </Modal>
      )}

      {/* Location Modal */}
      {isLocationModalOpen && (
        <Modal
          isOpen={isLocationModalOpen}
          title={editingLocId ? 'Edit Storage Location' : 'New Storage Location'}
          onClose={() => setIsLocationModalOpen(false)}
        >
          <form onSubmit={handleSaveLocation}>
            <div className="form-group" style={{ marginBottom: '14px' }}>
              <label className="form-label">Warehouse *</label>
              <select
                className="form-control"
                required
                value={locWarehouseId}
                onChange={(e) => setLocWarehouseId(Number(e.target.value))}
              >
                <option value="">Select Warehouse</option>
                {warehouses.map((w) => (
                  <option key={w.id} value={w.id}>
                    {w.name} ({w.code})
                  </option>
                ))}
              </select>
            </div>
            <div className="form-group" style={{ marginBottom: '14px' }}>
              <label className="form-label">Location Name *</label>
              <input
                type="text"
                className="form-control"
                required
                value={locName}
                onChange={(e) => setLocName(e.target.value)}
                placeholder="e.g. Rack A - Shelf 3 - Bin 12"
              />
            </div>
            <div className="form-group" style={{ marginBottom: '14px' }}>
              <label className="form-label">Unique Location Code *</label>
              <input
                type="text"
                className="form-control"
                required
                value={locCode}
                onChange={(e) => setLocCode(e.target.value.toUpperCase())}
                placeholder="e.g. LOC-RACK-A03"
              />
            </div>
            <div className="form-group" style={{ marginBottom: '20px' }}>
              <label className="form-label">Location Type</label>
              <select
                className="form-control"
                value={locType}
                onChange={(e) => setLocType(e.target.value as LocationType)}
              >
                <option value="INTERNAL">Internal Storage (Standard Inventory)</option>
                <option value="SUPPLIER">Supplier Virtual Location</option>
                <option value="CUSTOMER">Customer Outbound Location</option>
                <option value="TRANSIT">In-Transit Buffer</option>
                <option value="INVENTORY_LOSS">Inventory Loss / Write-off</option>
              </select>
            </div>
            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '10px' }}>
              <button
                type="button"
                className="btn btn-secondary"
                onClick={() => setIsLocationModalOpen(false)}
                disabled={locSubmitting}
              >
                Cancel
              </button>
              <button type="submit" className="btn btn-primary" disabled={locSubmitting}>
                {locSubmitting ? 'Saving...' : editingLocId ? 'Update' : 'Create Location'}
              </button>
            </div>
          </form>
        </Modal>
      )}
    </div>
  );
};

export default WarehousesPage;
