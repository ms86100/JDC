import React, { useState, useEffect } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import axiosClient from '../../../api/axiosClient';

interface SamlConfiguration {
  id?: string;
  registrationId: string;
  name: string;
  entityId: string;
  idpEntityId: string;
  idpSsoUrl: string;
  idpSloUrl?: string;
  idpCertificate: string;
  spEntityId?: string;
  acsUrl?: string;
  attributeMappingEmail: string;
  attributeMappingUsername: string;
  attributeMappingDisplayName: string;
  attributeMappingGroups: string;
  defaultRole: string;
  autoCreateUsers: boolean;
  enabled: boolean;
  forceAuthn: boolean;
  singleLogoutEnabled: boolean;
}

const defaultConfig: SamlConfiguration = {
  registrationId: '',
  name: '',
  entityId: '',
  idpEntityId: '',
  idpSsoUrl: '',
  idpSloUrl: '',
  idpCertificate: '',
  spEntityId: '',
  acsUrl: '',
  attributeMappingEmail: 'email',
  attributeMappingUsername: 'username',
  attributeMappingDisplayName: 'displayName',
  attributeMappingGroups: 'groups',
  defaultRole: 'ROLE_USER',
  autoCreateUsers: true,
  enabled: false,
  forceAuthn: false,
  singleLogoutEnabled: false,
};

const SamlConfigPage: React.FC = () => {
  const queryClient = useQueryClient();
  const [editingConfig, setEditingConfig] = useState<SamlConfiguration | null>(null);
  const [showForm, setShowForm] = useState(false);

  const { data: configs = [], isLoading } = useQuery<SamlConfiguration[]>({
    queryKey: ['saml-configs'],
    queryFn: async () => {
      const { data } = await axiosClient.get('/auth/api/admin/sso/saml');
      return data;
    },
  });

  const createMutation = useMutation({
    mutationFn: (config: SamlConfiguration) =>
      axiosClient.post('/auth/api/admin/sso/saml', config),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['saml-configs'] });
      setShowForm(false);
      setEditingConfig(null);
    },
  });

  const updateMutation = useMutation({
    mutationFn: (config: SamlConfiguration) =>
      axiosClient.put(`/auth/api/admin/sso/saml/${config.id}`, config),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['saml-configs'] });
      setShowForm(false);
      setEditingConfig(null);
    },
  });

  const deleteMutation = useMutation({
    mutationFn: (id: string) =>
      axiosClient.delete(`/auth/api/admin/sso/saml/${id}`),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['saml-configs'] }),
  });

  const testMutation = useMutation({
    mutationFn: (idpSsoUrl: string) =>
      axiosClient.post('/auth/api/admin/sso/saml/test', { idpSsoUrl }),
  });

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!editingConfig) return;
    if (editingConfig.id) {
      updateMutation.mutate(editingConfig);
    } else {
      createMutation.mutate(editingConfig);
    }
  };

  const handleEdit = (config: SamlConfiguration) => {
    setEditingConfig({ ...config });
    setShowForm(true);
  };

  const handleCreate = () => {
    setEditingConfig({ ...defaultConfig });
    setShowForm(true);
  };

  const updateField = (field: keyof SamlConfiguration, value: any) => {
    if (editingConfig) {
      setEditingConfig({ ...editingConfig, [field]: value });
    }
  };

  if (isLoading) return <div>Loading SAML configurations...</div>;

  return (
    <div style={{ padding: '24px' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '24px' }}>
        <h1 style={{ margin: 0 }}>SSO / SAML Configuration</h1>
        <button onClick={handleCreate} style={{ padding: '8px 16px', background: '#0052CC', color: '#fff', border: 'none', borderRadius: '3px', cursor: 'pointer' }}>
          Add Identity Provider
        </button>
      </div>

      {configs.length === 0 && !showForm && (
        <div style={{ padding: '40px', textAlign: 'center', background: '#f4f5f7', borderRadius: '3px' }}>
          <p>No SAML identity providers configured.</p>
          <p>Click "Add Identity Provider" to set up SSO.</p>
        </div>
      )}

      {configs.map((config) => (
        <div key={config.id} style={{ border: '1px solid #dfe1e6', borderRadius: '3px', padding: '16px', marginBottom: '12px' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <div>
              <strong>{config.name}</strong>
              <span style={{ marginLeft: '12px', padding: '2px 8px', borderRadius: '3px', fontSize: '12px', background: config.enabled ? '#00875a' : '#dfe1e6', color: config.enabled ? '#fff' : '#42526e' }}>
                {config.enabled ? 'Enabled' : 'Disabled'}
              </span>
            </div>
            <div>
              <button onClick={() => handleEdit(config)} style={{ marginRight: '8px', padding: '4px 12px', border: '1px solid #dfe1e6', borderRadius: '3px', cursor: 'pointer' }}>Edit</button>
              <button onClick={() => testMutation.mutate(config.idpSsoUrl)} style={{ marginRight: '8px', padding: '4px 12px', border: '1px solid #dfe1e6', borderRadius: '3px', cursor: 'pointer' }}>Test</button>
              <button onClick={() => config.id && deleteMutation.mutate(config.id)} style={{ padding: '4px 12px', border: '1px solid #de350b', borderRadius: '3px', color: '#de350b', cursor: 'pointer' }}>Delete</button>
            </div>
          </div>
          <div style={{ marginTop: '8px', fontSize: '13px', color: '#6b778c' }}>
            <div>Registration ID: {config.registrationId}</div>
            <div>IdP SSO URL: {config.idpSsoUrl}</div>
            <div>Entity ID: {config.entityId}</div>
          </div>
        </div>
      ))}

      {showForm && editingConfig && (
        <div style={{ border: '2px solid #0052CC', borderRadius: '3px', padding: '24px', marginTop: '16px' }}>
          <h2>{editingConfig.id ? 'Edit' : 'Add'} Identity Provider</h2>
          <form onSubmit={handleSubmit}>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px' }}>
              <div>
                <label style={{ display: 'block', fontWeight: 600, marginBottom: '4px' }}>Name *</label>
                <input type="text" value={editingConfig.name} onChange={(e) => updateField('name', e.target.value)} required style={{ width: '100%', padding: '8px', border: '1px solid #dfe1e6', borderRadius: '3px' }} />
              </div>
              <div>
                <label style={{ display: 'block', fontWeight: 600, marginBottom: '4px' }}>Registration ID *</label>
                <input type="text" value={editingConfig.registrationId} onChange={(e) => updateField('registrationId', e.target.value)} required disabled={!!editingConfig.id} style={{ width: '100%', padding: '8px', border: '1px solid #dfe1e6', borderRadius: '3px' }} />
              </div>
              <div>
                <label style={{ display: 'block', fontWeight: 600, marginBottom: '4px' }}>SP Entity ID *</label>
                <input type="text" value={editingConfig.entityId} onChange={(e) => updateField('entityId', e.target.value)} required style={{ width: '100%', padding: '8px', border: '1px solid #dfe1e6', borderRadius: '3px' }} />
              </div>
              <div>
                <label style={{ display: 'block', fontWeight: 600, marginBottom: '4px' }}>IdP Entity ID *</label>
                <input type="text" value={editingConfig.idpEntityId} onChange={(e) => updateField('idpEntityId', e.target.value)} required style={{ width: '100%', padding: '8px', border: '1px solid #dfe1e6', borderRadius: '3px' }} />
              </div>
              <div>
                <label style={{ display: 'block', fontWeight: 600, marginBottom: '4px' }}>IdP SSO URL *</label>
                <input type="url" value={editingConfig.idpSsoUrl} onChange={(e) => updateField('idpSsoUrl', e.target.value)} required style={{ width: '100%', padding: '8px', border: '1px solid #dfe1e6', borderRadius: '3px' }} />
              </div>
              <div>
                <label style={{ display: 'block', fontWeight: 600, marginBottom: '4px' }}>IdP SLO URL</label>
                <input type="url" value={editingConfig.idpSloUrl || ''} onChange={(e) => updateField('idpSloUrl', e.target.value)} style={{ width: '100%', padding: '8px', border: '1px solid #dfe1e6', borderRadius: '3px' }} />
              </div>
            </div>

            <div style={{ marginTop: '16px' }}>
              <label style={{ display: 'block', fontWeight: 600, marginBottom: '4px' }}>IdP Certificate (PEM) *</label>
              <textarea value={editingConfig.idpCertificate} onChange={(e) => updateField('idpCertificate', e.target.value)} required rows={6} style={{ width: '100%', padding: '8px', border: '1px solid #dfe1e6', borderRadius: '3px', fontFamily: 'monospace', fontSize: '12px' }} placeholder="-----BEGIN CERTIFICATE-----&#10;MIIDp...&#10;-----END CERTIFICATE-----" />
            </div>

            <h3 style={{ marginTop: '24px' }}>Attribute Mappings</h3>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px' }}>
              <div>
                <label style={{ display: 'block', fontWeight: 600, marginBottom: '4px' }}>Email Attribute</label>
                <input type="text" value={editingConfig.attributeMappingEmail} onChange={(e) => updateField('attributeMappingEmail', e.target.value)} style={{ width: '100%', padding: '8px', border: '1px solid #dfe1e6', borderRadius: '3px' }} />
              </div>
              <div>
                <label style={{ display: 'block', fontWeight: 600, marginBottom: '4px' }}>Username Attribute</label>
                <input type="text" value={editingConfig.attributeMappingUsername} onChange={(e) => updateField('attributeMappingUsername', e.target.value)} style={{ width: '100%', padding: '8px', border: '1px solid #dfe1e6', borderRadius: '3px' }} />
              </div>
              <div>
                <label style={{ display: 'block', fontWeight: 600, marginBottom: '4px' }}>Display Name Attribute</label>
                <input type="text" value={editingConfig.attributeMappingDisplayName} onChange={(e) => updateField('attributeMappingDisplayName', e.target.value)} style={{ width: '100%', padding: '8px', border: '1px solid #dfe1e6', borderRadius: '3px' }} />
              </div>
              <div>
                <label style={{ display: 'block', fontWeight: 600, marginBottom: '4px' }}>Groups Attribute</label>
                <input type="text" value={editingConfig.attributeMappingGroups} onChange={(e) => updateField('attributeMappingGroups', e.target.value)} style={{ width: '100%', padding: '8px', border: '1px solid #dfe1e6', borderRadius: '3px' }} />
              </div>
              <div>
                <label style={{ display: 'block', fontWeight: 600, marginBottom: '4px' }}>Default Role</label>
                <input type="text" value={editingConfig.defaultRole} onChange={(e) => updateField('defaultRole', e.target.value)} style={{ width: '100%', padding: '8px', border: '1px solid #dfe1e6', borderRadius: '3px' }} />
              </div>
            </div>

            <h3 style={{ marginTop: '24px' }}>Options</h3>
            <div style={{ display: 'flex', gap: '24px', flexWrap: 'wrap' }}>
              <label><input type="checkbox" checked={editingConfig.enabled} onChange={(e) => updateField('enabled', e.target.checked)} /> Enabled</label>
              <label><input type="checkbox" checked={editingConfig.autoCreateUsers} onChange={(e) => updateField('autoCreateUsers', e.target.checked)} /> Auto-create Users</label>
              <label><input type="checkbox" checked={editingConfig.forceAuthn} onChange={(e) => updateField('forceAuthn', e.target.checked)} /> Force Authentication</label>
              <label><input type="checkbox" checked={editingConfig.singleLogoutEnabled} onChange={(e) => updateField('singleLogoutEnabled', e.target.checked)} /> Single Logout</label>
            </div>

            <div style={{ marginTop: '24px', display: 'flex', gap: '8px' }}>
              <button type="submit" style={{ padding: '8px 16px', background: '#0052CC', color: '#fff', border: 'none', borderRadius: '3px', cursor: 'pointer' }}>
                {editingConfig.id ? 'Save Changes' : 'Create'}
              </button>
              <button type="button" onClick={() => { setShowForm(false); setEditingConfig(null); }} style={{ padding: '8px 16px', background: '#f4f5f7', border: '1px solid #dfe1e6', borderRadius: '3px', cursor: 'pointer' }}>
                Cancel
              </button>
            </div>
          </form>
        </div>
      )}
    </div>
  );
};

export default SamlConfigPage;
