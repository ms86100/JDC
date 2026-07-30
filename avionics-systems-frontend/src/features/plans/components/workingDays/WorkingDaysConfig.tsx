import React, { useState } from 'react';
import { useWorkingDays, useDefaultWorkingDays, useCreateWorkingDays, useUpdateWorkingDays, useAddHoliday, useRemoveHoliday } from '../../hooks/useWorkingDays';

interface WorkingDaysConfigProps {
  onClose: () => void;
}

export default function WorkingDaysConfig({ onClose }: WorkingDaysConfigProps) {
  const { data: workingDaysConfigs, isLoading } = useWorkingDays();
  const { data: defaultConfig } = useDefaultWorkingDays();

  const createWorkingDays = useCreateWorkingDays();
  const updateWorkingDays = useUpdateWorkingDays();
  const addHoliday = useAddHoliday();
  const removeHoliday = useRemoveHoliday();

  const [selectedConfigId, setSelectedConfigId] = useState<string | null>(null);
  const [formData, setFormData] = useState({
    name: '',
    monday: true,
    tuesday: true,
    wednesday: true,
    thursday: true,
    friday: true,
    saturday: false,
    sunday: false,
    hoursPerDay: 8,
    isDefault: false,
  });
  const [newHoliday, setNewHoliday] = useState({ date: '', name: '' });

  const selectedConfig = workingDaysConfigs?.find(w => w.id === selectedConfigId);

  if (isLoading) {
    return <div className="ab-config-loading">Loading working days configuration...</div>;
  }

  const handleCreate = () => {
    createWorkingDays.mutate({ ...formData, hoursPerDay: formData.hoursPerDay });
    resetForm();
  };

  const handleUpdate = () => {
    if (!selectedConfigId) return;
    updateWorkingDays.mutate({ id: selectedConfigId, data: formData });
  };

  const handleSelectConfig = (config: typeof selectedConfig) => {
    if (!config) return;
    setSelectedConfigId(config.id);
    setFormData({
      name: config.name,
      monday: config.monday,
      tuesday: config.tuesday,
      wednesday: config.wednesday,
      thursday: config.thursday,
      friday: config.friday,
      saturday: config.saturday,
      sunday: config.sunday,
      hoursPerDay: config.hoursPerDay,
      isDefault: config.isDefault,
    });
  };

  const handleAddHoliday = () => {
    if (!selectedConfigId || !newHoliday.date) return;
    addHoliday.mutate({
      configId: selectedConfigId,
      data: { date: newHoliday.date, name: newHoliday.name },
    });
    setNewHoliday({ date: '', name: '' });
  };

  const handleRemoveHoliday = (holidayId: string) => {
    if (!selectedConfigId) return;
    removeHoliday.mutate({ configId: selectedConfigId, holidayId });
  };

  const resetForm = () => {
    setSelectedConfigId(null);
    setFormData({
      name: '',
      monday: true,
      tuesday: true,
      wednesday: true,
      thursday: true,
      friday: true,
      saturday: false,
      sunday: false,
      hoursPerDay: 8,
      isDefault: false,
    });
  };

  return (
    <div className="ab-working-days-config">
      <div className="ab-config-header">
        <h2>Working Days Configuration</h2>
        <button className="ab-btn-close" onClick={onClose}>&times;</button>
      </div>

      <div className="ab-config-content">
        {/* Config List */}
        <div className="ab-configs-list">
          <h3>Calendar Configurations</h3>
          {workingDaysConfigs?.map(config => (
            <div
              key={config.id}
              className={`ab-config-item ${selectedConfigId === config.id ? 'ab-selected' : ''}`}
              onClick={() => handleSelectConfig(config)}
            >
              <span className="ab-config-name">
                {config.name}
                {config.isDefault && <span className="ab-badge">Default</span>}
              </span>
              <span className="ab-config-info">
                {config.workingDaysPerWeek} days/week, {config.hoursPerDay}h/day
              </span>
            </div>
          ))}
          <button className="ab-btn ab-btn-secondary" onClick={resetForm}>
            Create New
          </button>
        </div>

        {/* Config Editor */}
        <div className="ab-config-editor">
          <h3>{selectedConfigId ? 'Edit Configuration' : 'New Configuration'}</h3>

          <div className="ab-form-group">
            <label>Name</label>
            <input
              type="text"
              className="ab-input"
              value={formData.name}
              onChange={(e) => setFormData({ ...formData, name: e.target.value })}
              placeholder="e.g., Standard 5-Day Week"
            />
          </div>

          <div className="ab-form-group">
            <label>Working Days</label>
            <div className="ab-days-selector">
              {['monday', 'tuesday', 'wednesday', 'thursday', 'friday', 'saturday', 'sunday'].map(day => (
                <label key={day} className={`ab-day-checkbox ${formData[day as keyof typeof formData] ? 'ab-active' : ''}`}>
                  <input
                    type="checkbox"
                    checked={formData[day as keyof typeof formData] as boolean}
                    onChange={(e) => setFormData({ ...formData, [day]: e.target.checked })}
                  />
                  <span>{day.slice(0, 3).toUpperCase()}</span>
                </label>
              ))}
            </div>
          </div>

          <div className="ab-form-group">
            <label>Hours per Day</label>
            <input
              type="number"
              className="ab-input"
              value={formData.hoursPerDay}
              onChange={(e) => setFormData({ ...formData, hoursPerDay: Number(e.target.value) })}
              min={0}
              max={24}
              step={0.5}
            />
          </div>

          <div className="ab-form-group">
            <label className="ab-checkbox-label">
              <input
                type="checkbox"
                checked={formData.isDefault}
                onChange={(e) => setFormData({ ...formData, isDefault: e.target.checked })}
              />
              Set as default configuration
            </label>
          </div>

          <div className="ab-form-actions">
            {selectedConfigId ? (
              <button className="ab-btn ab-btn-primary" onClick={handleUpdate}>
                Save Changes
              </button>
            ) : (
              <button className="ab-btn ab-btn-primary" onClick={handleCreate}>
                Create
              </button>
            )}
            <button className="ab-btn ab-btn-secondary" onClick={resetForm}>
              Cancel
            </button>
          </div>

          {/* Holidays Section */}
          {selectedConfigId && selectedConfig && (
            <div className="ab-holidays-section">
              <h4>Holidays</h4>
              <div className="ab-holidays-list">
                {selectedConfig.holidays?.map(holiday => (
                  <div key={holiday.id} className="ab-holiday-item">
                    <span className="ab-holiday-date">{holiday.date}</span>
                    <span className="ab-holiday-name">{holiday.name}</span>
                    <button
                      className="ab-btn ab-btn-sm ab-btn-danger"
                      onClick={() => handleRemoveHoliday(holiday.id)}
                    >
                      Remove
                    </button>
                  </div>
                ))}
              </div>
              <div className="ab-add-holiday">
                <input
                  type="date"
                  className="ab-input"
                  value={newHoliday.date}
                  onChange={(e) => setNewHoliday({ ...newHoliday, date: e.target.value })}
                />
                <input
                  type="text"
                  className="ab-input"
                  placeholder="Holiday name"
                  value={newHoliday.name}
                  onChange={(e) => setNewHoliday({ ...newHoliday, name: e.target.value })}
                />
                <button className="ab-btn ab-btn-primary" onClick={handleAddHoliday}>
                  Add Holiday
                </button>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}