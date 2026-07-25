import { useState, useEffect } from 'react';
import { useSearchParams } from 'react-router-dom';
import { changeManagementApi } from '../../../api/changeManagementApi';
import { DEMO_CHANGE_CARDS, DEMO_DESIGN_ITEMS } from '../demoData';
import '../AircraftDesignStyles.css';

const TABS = ['Design', 'EIF', 'Planning', 'Review', 'Certification', 'Maturity Test', 'Safety'] as const;

const EASA_CLASSIFICATIONS = ['Minor', 'Major', 'Significant', 'Critical'] as const;
const RAG_STATUSES = ['Green', 'Amber', 'Red'] as const;

interface ChangeCardData {
  id?: string;
  issueId?: string;
  classification?: string;
  designReviewRag?: string;
  title?: string;
  description?: string;
  // Design tab
  designJustification?: string;
  designImpact?: string;
  affectedParts?: string;
  // EIF tab
  eifReference?: string;
  eifStatus?: string;
  // Planning tab
  targetDate?: string;
  milestones?: string;
  // Review tab
  reviewStatus?: string;
  reviewComments?: string;
  // Certification tab
  certificationBasis?: string;
  complianceMethod?: string;
  // Maturity Test tab
  maturityTestPlan?: string;
  maturityTestStatus?: string;
  // Safety tab
  safetyAssessment?: string;
  safetyClassification?: string;
}

interface DesignItemData {
  id?: string;
  partNumber?: string;
  partName?: string;
  revision?: string;
  status?: string;
}

export default function ChangeCardPage() {
  const [searchParams] = useSearchParams();
  const issueId = searchParams.get('issueId') || '';

  const [activeTab, setActiveTab] = useState<typeof TABS[number]>('Design');
  const [changeCard, setChangeCard] = useState<ChangeCardData | null>(null);
  const [designItem, setDesignItem] = useState<DesignItemData | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [editing, setEditing] = useState(false);
  const [editForm, setEditForm] = useState<ChangeCardData>({});
  const [saving, setSaving] = useState(false);
  const [issueInput, setIssueInput] = useState(issueId);

  useEffect(() => {
    if (issueId) loadChangeCard(issueId);
    else loadDemoData();
  }, [issueId]);

  function loadDemoData() {
    const demo = DEMO_CHANGE_CARDS[0];
    setChangeCard(demo);
    setEditForm(demo);
    setDesignItem(DEMO_DESIGN_ITEMS[0]);
    setIssueInput(demo.issueId || 'cc001');
  }

  async function loadChangeCard(id: string) {
    setLoading(true);
    setError('');
    try {
      const res = await changeManagementApi.getChangeCard(id);
      setChangeCard(res.data);
      setEditForm(res.data || {});
    } catch (err: any) {
      loadDemoData();
    }
    try {
      const diRes = await changeManagementApi.getDesignItem(id);
      setDesignItem(diRes.data);
    } catch {
      setDesignItem(null);
    }
    setLoading(false);
  }

  async function handleCreateChangeCard() {
    if (!issueInput) return;
    setSaving(true);
    try {
      const res = await changeManagementApi.createChangeCard(issueInput, editForm);
      setChangeCard(res.data);
      setEditing(false);
    } catch (err: any) {
      alert(err?.response?.data?.message || 'Failed to create Change Card');
    } finally {
      setSaving(false);
    }
  }

  async function handleSave() {
    if (!issueInput) return;
    setSaving(true);
    try {
      const res = await changeManagementApi.updateChangeCard(issueInput, editForm);
      setChangeCard(res.data);
      setEditing(false);
    } catch (err: any) {
      alert(err?.response?.data?.message || 'Save failed');
    } finally {
      setSaving(false);
    }
  }

  function handleLoad() {
    if (issueInput) loadChangeCard(issueInput);
  }

  const updateField = (key: keyof ChangeCardData, value: string) => {
    setEditForm(f => ({ ...f, [key]: value }));
  };

  function renderField(label: string, key: keyof ChangeCardData, type: 'input' | 'textarea' | 'select' = 'input', options?: readonly string[]) {
    return (
      <div className="ads-field" style={{ marginBottom: 12 }}>
        <span className="ads-field-label">{label}</span>
        {editing ? (
          type === 'textarea' ? (
            <textarea className="ads-field-textarea" value={(editForm[key] as string) || ''} onChange={e => updateField(key, e.target.value)} />
          ) : type === 'select' && options ? (
            <select className="ads-select" value={(editForm[key] as string) || ''} onChange={e => updateField(key, e.target.value)}>
              <option value="">-- Select --</option>
              {options.map(o => <option key={o} value={o}>{o}</option>)}
            </select>
          ) : (
            <input className="ads-field-input" value={(editForm[key] as string) || ''} onChange={e => updateField(key, e.target.value)} />
          )
        ) : (
          <span className="ads-field-value">{(changeCard?.[key] as string) || '-'}</span>
        )}
      </div>
    );
  }

  function renderTabContent() {
    switch (activeTab) {
      case 'Design':
        return (
          <div className="ads-fields">
            {renderField('Design Justification', 'designJustification', 'textarea')}
            {renderField('Design Impact', 'designImpact', 'textarea')}
            {renderField('Affected Parts', 'affectedParts', 'textarea')}
          </div>
        );
      case 'EIF':
        return (
          <div className="ads-fields">
            {renderField('EIF Reference', 'eifReference')}
            {renderField('EIF Status', 'eifStatus')}
          </div>
        );
      case 'Planning':
        return (
          <div className="ads-fields">
            {renderField('Target Date', 'targetDate')}
            {renderField('Milestones', 'milestones', 'textarea')}
          </div>
        );
      case 'Review':
        return (
          <div className="ads-fields">
            {renderField('Review Status', 'reviewStatus')}
            {renderField('Review Comments', 'reviewComments', 'textarea')}
            {renderField('Design Review RAG', 'designReviewRag', 'select', RAG_STATUSES)}
          </div>
        );
      case 'Certification':
        return (
          <div className="ads-fields">
            {renderField('Certification Basis', 'certificationBasis', 'textarea')}
            {renderField('Compliance Method', 'complianceMethod')}
          </div>
        );
      case 'Maturity Test':
        return (
          <div className="ads-fields">
            {renderField('Maturity Test Plan', 'maturityTestPlan', 'textarea')}
            {renderField('Maturity Test Status', 'maturityTestStatus')}
          </div>
        );
      case 'Safety':
        return (
          <div className="ads-fields">
            {renderField('Safety Assessment', 'safetyAssessment', 'textarea')}
            {renderField('Safety Classification', 'safetyClassification')}
          </div>
        );
      default:
        return null;
    }
  }

  return (
    <div className="ads-page">
      <h1 className="ads-page-title">Change Card Management</h1>

      {/* Issue selector */}
      <div className="ads-card" style={{ marginBottom: 20 }}>
        <div className="ads-toolbar">
          <input
            className="ads-search-input"
            placeholder="Enter Issue ID..."
            value={issueInput}
            onChange={e => setIssueInput(e.target.value)}
          />
          <button className="ads-btn ads-btn--primary" onClick={handleLoad}>Load</button>
        </div>
      </div>

      {loading && <div className="ads-loading"><div className="ab-spinner" /> Loading...</div>}
      {error && <div className="ads-alert ads-alert--error">{error}</div>}

      {!loading && issueInput && !changeCard && !error && (
        <div className="ads-card" style={{ textAlign: 'center' }}>
          <p style={{ color: '#6b778c', marginBottom: 12 }}>No Change Card found for this issue.</p>
          <button className="ads-btn ads-btn--primary" onClick={() => { setEditing(true); handleCreateChangeCard(); }}>
            Create Change Card
          </button>
        </div>
      )}

      {changeCard && (
        <>
          {/* Header with classification */}
          <div className="ads-detail-header">
            <div>
              <div className="ads-detail-meta">
                <span className="ads-detail-key">{changeCard.issueId || issueInput}</span>
                {changeCard.classification && (
                  <span className={`ads-badge ads-badge--${changeCard.classification === 'Critical' ? 'cancelled' : changeCard.classification === 'Major' ? 'to_be_verified' : 'verified'}`}>
                    {changeCard.classification}
                  </span>
                )}
                {changeCard.designReviewRag && (
                  <span style={{
                    display: 'inline-block',
                    width: 14,
                    height: 14,
                    borderRadius: '50%',
                    background: changeCard.designReviewRag === 'Green' ? '#00875a' : changeCard.designReviewRag === 'Amber' ? '#ff8b00' : '#de350b',
                    marginLeft: 8,
                  }} title={`RAG: ${changeCard.designReviewRag}`} />
                )}
              </div>
              <h2 className="ads-detail-summary">{changeCard.title || 'Change Card'}</h2>
            </div>
            <div className="ads-detail-actions">
              {editing ? (
                <>
                  <button className="ads-btn" onClick={() => { setEditing(false); setEditForm(changeCard); }}>Cancel</button>
                  <button className="ads-btn ads-btn--primary" onClick={handleSave} disabled={saving}>
                    {saving ? 'Saving...' : 'Save'}
                  </button>
                </>
              ) : (
                <button className="ads-btn" onClick={() => setEditing(true)}>Edit</button>
              )}
            </div>
          </div>

          {/* Classification */}
          <div className="ads-section">
            <h3 className="ads-section-title">Classification</h3>
            <div className="ads-fields">
              {renderField('EASA Classification', 'classification', 'select', EASA_CLASSIFICATIONS)}
              {renderField('Description', 'description', 'textarea')}
            </div>
          </div>

          {/* Design Item info */}
          {designItem && (
            <div className="ads-section">
              <h3 className="ads-section-title">Design Item</h3>
              <div className="ads-fields">
                <div className="ads-field">
                  <span className="ads-field-label">Part Number</span>
                  <span className="ads-field-value">{designItem.partNumber || '-'}</span>
                </div>
                <div className="ads-field">
                  <span className="ads-field-label">Part Name</span>
                  <span className="ads-field-value">{designItem.partName || '-'}</span>
                </div>
                <div className="ads-field">
                  <span className="ads-field-label">Revision</span>
                  <span className="ads-field-value">{designItem.revision || '-'}</span>
                </div>
                <div className="ads-field">
                  <span className="ads-field-label">Status</span>
                  <span className="ads-field-value">{designItem.status || '-'}</span>
                </div>
              </div>
            </div>
          )}

          {/* Tabs */}
          <div className="ads-tabs">
            {TABS.map(tab => (
              <button
                key={tab}
                className={`ads-tab${activeTab === tab ? ' ads-tab--active' : ''}`}
                onClick={() => setActiveTab(tab)}
              >
                {tab}
              </button>
            ))}
          </div>

          <div className="ads-card">
            {renderTabContent()}
          </div>
        </>
      )}
    </div>
  );
}
